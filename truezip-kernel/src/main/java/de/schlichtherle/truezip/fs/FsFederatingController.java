/*
 * Copyright (C) 2011 Schlichtherle IT Services
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
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.Map;
import javax.swing.Icon;
import net.jcip.annotations.ThreadSafe;

/**
 * Implements a chain of responsibility in order to resolve
 * {@link FsFalsePositiveException}s thrown by the prospective file system
 * provided to its {@link #FsFederatingController constructor}.
 * <p>
 * Whenever the controller for the prospective file system throws a
 * {@link FsFalsePositiveException}, the method call is delegated to the
 * controller for its parent file system in order to resolve the requested
 * operation.
 * If this method call fails with another exception, then the
 * {@link IOException} which is associated as the cause of the first exception
 * gets rethrown unless the second exception is an {@link FsException} again.
 * In this case the {@link FsException} gets rethrown as is in order to enable
 * the caller to resolve it, which is typically - but not necessarily - the
 * TrueZIP Kernel again.
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
 *     themselves by the type {@link FsException} and are excempt from this
 *     masquerade in order to support resolving them by a more competent caller.
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
public final class FsFederatingController
extends FsDecoratingController<FsModel, FsController<?>> {

    private volatile @CheckForNull FsPath path;

    /**
     * Constructs a new file system federating controller.
     *
     * @param controller the decorated file system controller.
     */
    FsFederatingController(final FsController<?> controller) {
        super(controller);
        assert null != getParent();
    }

    private FsEntryName resolveParent(FsEntryName name) {
        return getPath().resolve(name).getEntryName();
    }

    private FsPath getPath() {
        final FsPath path = this.path;
        return null != path ? path : (this.path = getMountPoint().getPath());
    }

    @Override
    public Icon getOpenIcon() throws IOException {
        try {
            return delegate.getOpenIcon();
        } catch (FsFalsePositiveException ex) {
            try {
                return getParent().getOpenIcon();
            } catch (FsException ex2) {
                assert !(ex2 instanceof FsFalsePositiveException);
                throw ex2;
            } catch (IOException discard) {
                throw ex.getCause();
            }
        }
    }

    @Override
    public Icon getClosedIcon() throws IOException {
        try {
            return delegate.getClosedIcon();
        } catch (FsFalsePositiveException ex) {
            try {
                return getParent().getClosedIcon();
            } catch (FsException ex2) {
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
            } catch (FsException ex2) {
                assert !(ex2 instanceof FsFalsePositiveException);
                throw ex2;
            } catch (IOException discard) {
                throw ex.getCause();
            }
        }
    }

    @Override
    public FsEntry getEntry(FsEntryName name) throws IOException {
        try {
            return delegate.getEntry(name);
        } catch (FsFalsePositiveException ex) {
            try {
                return getParent().getEntry(resolveParent(name));
            } catch (FsException ex2) {
                assert !(ex2 instanceof FsFalsePositiveException);
                throw ex2;
            } catch (IOException discard) {
                throw ex.getCause();
            }
        }
    }

    @Override
    public boolean isReadable(FsEntryName name) throws IOException {
        try {
            return delegate.isReadable(name);
        } catch (FsFalsePositiveException ex) {
            try {
                return getParent().isReadable(resolveParent(name));
            } catch (FsException ex2) {
                assert !(ex2 instanceof FsFalsePositiveException);
                throw ex2;
            } catch (IOException discard) {
                throw ex.getCause();
            }
        }
    }

    @Override
    public boolean isWritable(FsEntryName name) throws IOException {
        try {
            return delegate.isWritable(name);
        } catch (FsFalsePositiveException ex) {
            try {
                return getParent().isWritable(resolveParent(name));
            } catch (FsException ex2) {
                assert !(ex2 instanceof FsFalsePositiveException);
                throw ex2;
            } catch (IOException discard) {
                throw ex.getCause();
            }
        }
    }

    @Override
    public void setReadOnly(FsEntryName name) throws IOException {
        try {
            delegate.setReadOnly(name);
        } catch (FsFalsePositiveException ex) {
            try {
                getParent().setReadOnly(resolveParent(name));
            } catch (FsException ex2) {
                assert !(ex2 instanceof FsFalsePositiveException);
                throw ex2;
            } catch (IOException discard) {
                throw ex.getCause();
            }
        }
    }

    @Override
    public boolean setTime(
            FsEntryName name,
            Map<Access, Long> times,
            BitField<FsOutputOption> options)
    throws IOException {
        try {
            return delegate.setTime(name, times, options);
        } catch (FsFalsePositiveException ex) {
            try {
                return getParent().setTime(resolveParent(name), times, options);
            } catch (FsException ex2) {
                assert !(ex2 instanceof FsFalsePositiveException);
                throw ex2;
            } catch (IOException discard) {
                throw ex.getCause();
            }
        }
    }

    @Override
    public boolean setTime(
            FsEntryName name,
            BitField<Access> types,
            long value,
            BitField<FsOutputOption> options)
    throws IOException {
        try {
            return delegate.setTime(name, types, value, options);
        } catch (FsFalsePositiveException ex) {
            try {
                return getParent().setTime(resolveParent(name), types, value, options);
            } catch (FsException ex2) {
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
            FsEntryName name,
            BitField<FsOutputOption> options,
            @CheckForNull Entry template) {
        return new Output(  delegate.getOutputSocket(name, options, template),
                            name, options, template);
    }

    @Override
    public void mknod(
            FsEntryName name,
            Type type,
            BitField<FsOutputOption> options,
            @CheckForNull Entry template)
    throws IOException {
        try {
            delegate.mknod(name, type, options, template);
        } catch (FsFalsePositiveException ex) {
            try {
                getParent().mknod(resolveParent(name), type, options, template);
            } catch (FsException ex2) {
                assert !(ex2 instanceof FsFalsePositiveException);
                throw ex2;
            } catch (IOException discard) {
                throw ex.getCause();
            }
        }
    }

    @Override
    public void unlink(FsEntryName name, BitField<FsOutputOption> options)
    throws IOException {
        try {
            delegate.unlink(name, options);
        } catch (FsFalsePositiveException ex) {
            try {
                getParent().unlink(resolveParent(name), options);
            } catch (FsException ex2) {
                assert !(ex2 instanceof FsFalsePositiveException);
                throw ex2;
            } catch (IOException discard) {
                throw ex.getCause();
            }
        }
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
                } catch (FsException ex2) {
                    assert !(ex2 instanceof FsFalsePositiveException);
                    throw ex2;
                } catch (IOException discard) {
                    throw ex.getCause();
                }
            }
        }

        @Override
        public Entry getPeerTarget() throws IOException {
            // Same implementation as super class, but makes stack trace nicer.
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
                } catch (FsException ex2) {
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
                } catch (FsException ex2) {
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
                } catch (FsException ex2) {
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
                } catch (FsException ex2) {
                    assert !(ex2 instanceof FsFalsePositiveException);
                    throw ex2;
                } catch (IOException discard) {
                    throw ex.getCause();
                }
            }
        }

        @Override
        public Entry getPeerTarget() throws IOException {
            // Same implementation as super class, but makes stack trace nicer.
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
                } catch (FsException ex2) {
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
                } catch (FsException ex2) {
                    assert !(ex2 instanceof FsFalsePositiveException);
                    throw ex2;
                } catch (IOException discard) {
                    throw ex.getCause();
                }
            }
        }
    } // Output
}
