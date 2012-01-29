/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.entry.Entry.Access;
import de.schlichtherle.truezip.entry.Entry.Type;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.socket.DecoratingInputSocket;
import de.schlichtherle.truezip.socket.DecoratingOutputSocket;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.ExceptionHandler;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.Map;
import javax.swing.Icon;
import net.jcip.annotations.ThreadSafe;

/**
 * Implements a chain of responsibility in order to resolve
 * {@link FsFalsePositiveException}s thrown by the prospective file system
 * provided to its {@link #FsFalsePositiveController constructor}.
 * <p>
 * Whenever the controller for the prospective file system throws a
 * {@link FsFalsePositiveException}, the method call is delegated to the
 * controller for its parent file system in order to resolve the requested
 * operation.
 * If this method call fails with a second exception, then the
 * {@link IOException} which is associated as the cause of the first exception
 * gets rethrown unless the second exception is an
 * {@link FsControllerException}.
 * In this case the {@link FsControllerException} gets rethrown as is in order
 * to enable the caller to resolve it.
 * <p>
 * This algorithm effectively achieves the following objectives:
 * <ol>
 * <li>False positive federated file systems (i.e. false positive archive files)
 *     get resolved correctly by accessing them as entities of the parent file
 *     system.
 * <li>If the file system driver for the parent file system throws another
 *     exception, then it gets discarded and the exception initially thrown by
 *     the file system driver for the false positive archive file takes its
 *     place in order to provide the caller with a good indication of what went
 *     wrong in the first place.
 * <li>Exceptions which are thrown by the TrueZIP Kernel itself identify
 *     themselves by the type {@link FsControllerException}.
 *     They are excempt from this masquerade in order to support resolving them
 *     by a more competent caller.
 * </ol>
 * <p>
 * As an example consider the case of accessing a RAES encrypted ZIP file.
 * When an archive file of this type gets mounted, the user is typically
 * prompted for a password.
 * If the user cancels the password prompting dialog, then an appropriate
 * exception gets thrown.
 * Some other class in the TrueZIP Kernel would then catch this exception and
 * flag the archive file as a false positive by wrapping the exception in a
 * {@link FsFalsePositiveException}.
 * This class would then catch this false positive exception and try to resolve
 * the issue by using the parent file system controller.
 * Failing that, the initial exception would get rethrown in order to signal
 * to the caller that the user had cancelled password prompting.
 *
 * @see     FsFalsePositiveException
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@DefaultAnnotation(NonNull.class)
final class FsFalsePositiveController
extends FsDecoratingController<FsModel, FsController<?>> {

    // These fields don't need to be volatile because reads and writes of
    // references are always atomic.
    // See The Java Language Specification, Third Edition, section 17.7
    // "Non-atomic Treatment of double and long".
    private /*volatile*/ @CheckForNull FsController<?> parent;
    private /*volatile*/ @CheckForNull FsPath path;

    /**
     * Constructs a new false positive file system controller.
     *
     * @param controller the decorated file system controller.
     */
    FsFalsePositiveController(final FsController<?> controller) {
        super(controller);
        assert null != super.getParent();
    }

    @Override
    public FsController<?> getParent() {
        final FsController<?> parent = this.parent;
        return null != parent ? parent : (this.parent = delegate.getParent());
    }

    private FsEntryName resolveParent(FsEntryName name) {
        return getPath().resolve(name).getEntryName();
    }

    private FsPath getPath() {
        final FsPath path = this.path;
        return null != path ? path : (this.path = getMountPoint().getPath());
    }

    @Override
    @Deprecated
    public Icon getOpenIcon() throws IOException {
        try {
            return delegate.getOpenIcon();
        } catch (FsFalsePositiveException ex) {
            try {
                return getParent().getOpenIcon();
            } catch (FsControllerException ex2) {
                assert !(ex2 instanceof FsFalsePositiveException);
                throw ex2;
            } catch (IOException discard) {
                throw ex.getCause();
            }
        }
    }

    @Override
    @Deprecated
    public Icon getClosedIcon() throws IOException {
        try {
            return delegate.getClosedIcon();
        } catch (FsFalsePositiveException ex) {
            try {
                return getParent().getClosedIcon();
            } catch (FsControllerException ex2) {
                assert !(ex2 instanceof FsFalsePositiveException);
                throw ex2;
            } catch (IOException discard) {
                throw ex.getCause();
            }
        }
    }

    @Override
    public boolean isReadOnly() throws IOException {
        try {
            return delegate.isReadOnly();
        } catch (FsFalsePositiveException ex) {
            try {
                return getParent().isReadOnly();
            } catch (FsControllerException ex2) {
                assert !(ex2 instanceof FsFalsePositiveException);
                throw ex2;
            } catch (IOException discard) {
                throw ex.getCause();
            }
        }
    }

    @Override
    public FsEntry getEntry(final FsEntryName name) throws IOException {
        try {
            return delegate.getEntry(name);
        } catch (FsFalsePositiveException ex) {
            try {
                return getParent().getEntry(resolveParent(name));
            } catch (FsControllerException ex2) {
                assert !(ex2 instanceof FsFalsePositiveException);
                throw ex2;
            } catch (IOException discard) {
                throw ex.getCause();
            }
        }
    }

    @Override
    public boolean isReadable(final FsEntryName name) throws IOException {
        try {
            return delegate.isReadable(name);
        } catch (FsFalsePositiveException ex) {
            try {
                return getParent().isReadable(resolveParent(name));
            } catch (FsControllerException ex2) {
                assert !(ex2 instanceof FsFalsePositiveException);
                throw ex2;
            } catch (IOException discard) {
                throw ex.getCause();
            }
        }
    }

    @Override
    public boolean isWritable(final FsEntryName name) throws IOException {
        try {
            return delegate.isWritable(name);
        } catch (FsFalsePositiveException ex) {
            try {
                return getParent().isWritable(resolveParent(name));
            } catch (FsControllerException ex2) {
                assert !(ex2 instanceof FsFalsePositiveException);
                throw ex2;
            } catch (IOException discard) {
                throw ex.getCause();
            }
        }
    }

    @Override
    public boolean isExecutable(final FsEntryName name) throws IOException {
        try {
            return delegate.isExecutable(name);
        } catch (FsFalsePositiveException ex) {
            try {
                return getParent().isExecutable(resolveParent(name));
            } catch (FsControllerException ex2) {
                assert !(ex2 instanceof FsFalsePositiveException);
                throw ex2;
            } catch (IOException discard) {
                throw ex.getCause();
            }
        }
    }

    @Override
    public void setReadOnly(final FsEntryName name) throws IOException {
        try {
            delegate.setReadOnly(name);
        } catch (FsFalsePositiveException ex) {
            try {
                getParent().setReadOnly(resolveParent(name));
            } catch (FsControllerException ex2) {
                assert !(ex2 instanceof FsFalsePositiveException);
                throw ex2;
            } catch (IOException discard) {
                throw ex.getCause();
            }
        }
    }

    @Override
    public boolean setTime(
            final FsEntryName name,
            final Map<Access, Long> times,
            final BitField<FsOutputOption> options)
    throws IOException {
        try {
            return delegate.setTime(name, times, options);
        } catch (FsFalsePositiveException ex) {
            try {
                return getParent().setTime(resolveParent(name), times, options);
            } catch (FsControllerException ex2) {
                assert !(ex2 instanceof FsFalsePositiveException);
                throw ex2;
            } catch (IOException discard) {
                throw ex.getCause();
            }
        }
    }

    @Override
    public boolean setTime(
            final FsEntryName name,
            final BitField<Access> types,
            final long value,
            final BitField<FsOutputOption> options)
    throws IOException {
        try {
            return delegate.setTime(name, types, value, options);
        } catch (FsFalsePositiveException ex) {
            try {
                return getParent().setTime(resolveParent(name), types, value, options);
            } catch (FsControllerException ex2) {
                assert !(ex2 instanceof FsFalsePositiveException);
                throw ex2;
            } catch (IOException discard) {
                throw ex.getCause();
            }
        }
    }

    @Override
    public InputSocket<?> getInputSocket(
            final FsEntryName name,
            final BitField<FsInputOption> options) {
        return new Input(   delegate.getInputSocket(name, options),
                            name, options);
    }

    @Override
    public OutputSocket<?> getOutputSocket(
            final FsEntryName name,
            final BitField<FsOutputOption> options,
            final @CheckForNull Entry template) {
        return new Output(  delegate.getOutputSocket(name, options, template),
                            name, options, template);
    }

    @Override
    public void mknod(
            final FsEntryName name,
            final Type type,
            final BitField<FsOutputOption> options,
            final @CheckForNull Entry template)
    throws IOException {
        try {
            delegate.mknod(name, type, options, template);
        } catch (FsFalsePositiveException ex) {
            try {
                getParent().mknod(resolveParent(name), type, options, template);
            } catch (FsControllerException ex2) {
                assert !(ex2 instanceof FsFalsePositiveException);
                throw ex2;
            } catch (IOException discard) {
                throw ex.getCause();
            }
        }
    }

    @Override
    public void unlink( final FsEntryName name,
                        final BitField<FsOutputOption> options)
    throws IOException {
        try {
            delegate.unlink(name, options);
        } catch (FsFalsePositiveException ex) {
            try {
                getParent().unlink(resolveParent(name), options);
            } catch (FsControllerException ex2) {
                assert !(ex2 instanceof FsFalsePositiveException);
                throw ex2;
            } catch (IOException discard) {
                throw ex.getCause();
            }
        }
    }

    @Override
    public <X extends IOException> void
    sync(   final BitField<FsSyncOption> options,
            final ExceptionHandler<? super FsSyncException, X> handler)
    throws X {
        // Mind there's no FsFalsePositiveException in the throws-declaration!
        delegate.sync(options, handler);
    }

    private final class Input extends DecoratingInputSocket<Entry> {
        final FsEntryName name;
        final BitField<FsInputOption> options;

        Input(  final InputSocket<?> input,
                final FsEntryName name,
                final BitField<FsInputOption> options) {
            super(input);
            this.name = name;
            this.options = options;
        }

        @Override
        public Entry getLocalTarget() throws IOException {
            try {
                return getBoundSocket().getLocalTarget();
            } catch (FsFalsePositiveException ex) {
                try {
                    return getParent()
                            .getInputSocket(resolveParent(name), options)
                            .bind(this)
                            .getLocalTarget();
                } catch (FsControllerException ex2) {
                    assert !(ex2 instanceof FsFalsePositiveException);
                    throw ex2;
                } catch (IOException discard) {
                    throw ex.getCause();
                }
            }
        }

        @Override
        public Entry getPeerTarget() throws IOException {
            return getBoundSocket().getPeerTarget();
        }

        @Override
        public ReadOnlyFile newReadOnlyFile() throws IOException {
            try {
                return getBoundSocket().newReadOnlyFile();
            } catch (FsFalsePositiveException ex) {
                try {
                    return getParent()
                            .getInputSocket(resolveParent(name), options)
                            .bind(this)
                            .newReadOnlyFile();
                } catch (FsControllerException ex2) {
                    assert !(ex2 instanceof FsFalsePositiveException);
                    throw ex2;
                } catch (IOException discard) {
                    throw ex.getCause();
                }
            }
        }

        @Override
        public SeekableByteChannel newSeekableByteChannel() throws IOException {
            try {
                return getBoundSocket().newSeekableByteChannel();
            } catch (FsFalsePositiveException ex) {
                try {
                    return getParent()
                            .getInputSocket(resolveParent(name), options)
                            .bind(this)
                            .newSeekableByteChannel();
                } catch (FsControllerException ex2) {
                    assert !(ex2 instanceof FsFalsePositiveException);
                    throw ex2;
                } catch (IOException discard) {
                    throw ex.getCause();
                }
            }
        }

        @Override
        public InputStream newInputStream() throws IOException {
            try {
                return getBoundSocket().newInputStream();
            } catch (FsFalsePositiveException ex) {
                try {
                    return getParent()
                            .getInputSocket(resolveParent(name), options)
                            .bind(this)
                            .newInputStream();
                } catch (FsControllerException ex2) {
                    assert !(ex2 instanceof FsFalsePositiveException);
                    throw ex2;
                } catch (IOException discard) {
                    throw ex.getCause();
                }
            }
        }
    } // Input

    private final class Output extends DecoratingOutputSocket<Entry> {
        final FsEntryName name;
        final BitField<FsOutputOption> options;
        final @CheckForNull Entry template;

        Output( final OutputSocket<?> output,
                final FsEntryName name,
                final BitField<FsOutputOption> options,
                final @CheckForNull Entry template) {
            super(output);
            this.name = name;
            this.options = options;
            this.template = template;
        }

        @Override
        public Entry getLocalTarget() throws IOException {
            try {
                return getBoundSocket().getLocalTarget();
            } catch (FsFalsePositiveException ex) {
                try {
                    return getParent()
                            .getOutputSocket(resolveParent(name), options, template)
                            .bind(this)
                            .getLocalTarget();
                } catch (FsControllerException ex2) {
                    assert !(ex2 instanceof FsFalsePositiveException);
                    throw ex2;
                } catch (IOException discard) {
                    throw ex.getCause();
                }
            }
        }

        @Override
        public Entry getPeerTarget() throws IOException {
            return getBoundSocket().getPeerTarget();
        }

        @Override
        public SeekableByteChannel newSeekableByteChannel() throws IOException {
            try {
                return getBoundSocket().newSeekableByteChannel();
            } catch (FsFalsePositiveException ex) {
                try {
                    return getParent()
                            .getOutputSocket(resolveParent(name), options, template)
                            .bind(this)
                            .newSeekableByteChannel();
                } catch (FsControllerException ex2) {
                    assert !(ex2 instanceof FsFalsePositiveException);
                    throw ex2;
                } catch (IOException discard) {
                    throw ex.getCause();
                }
            }
        }

        @Override
        public OutputStream newOutputStream() throws IOException {
            try {
                return getBoundSocket().newOutputStream();
            } catch (FsFalsePositiveException ex) {
                try {
                    return getParent()
                            .getOutputSocket(resolveParent(name), options, template)
                            .bind(this)
                            .newOutputStream();
                } catch (FsControllerException ex2) {
                    assert !(ex2 instanceof FsFalsePositiveException);
                    throw ex2;
                } catch (IOException discard) {
                    throw ex.getCause();
                }
            }
        }
    } // Output
}
