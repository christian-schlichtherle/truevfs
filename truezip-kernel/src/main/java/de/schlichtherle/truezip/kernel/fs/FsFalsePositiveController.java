/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.kernel.fs;

import de.truezip.kernel.cio.Entry;
import de.truezip.kernel.cio.Entry.Access;
import de.truezip.kernel.cio.Entry.Type;
import de.truezip.kernel.cio.InputSocket;
import de.truezip.kernel.cio.OutputSocket;
import de.truezip.kernel.fs.*;
import de.truezip.kernel.fs.addr.FsEntryName;
import static de.truezip.kernel.fs.addr.FsEntryName.ROOT;
import de.truezip.kernel.fs.addr.FsPath;
import de.truezip.kernel.fs.option.FsInputOption;
import de.truezip.kernel.fs.option.FsOutputOption;
import de.truezip.kernel.fs.option.FsSyncOption;
import de.truezip.kernel.rof.ReadOnlyFile;
import de.truezip.kernel.util.BitField;
import de.truezip.kernel.util.ExceptionHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Implements a chain of responsibility for resolving
 * {@link FsFalsePositiveException}s which may get thrown by its decorated file
 * system controller.
 * <p>
 * Whenever the decorated controller for the prospective file system throws a
 * {@link FsFalsePositiveException}, the file system operation is routed to the
 * controller of the parent file system in order to continue the operation.
 * If this fails with another exception, then the {@link IOException} which is
 * associated as the cause of the initial exception gets rethrown unless the
 * other exception is an {@link FsControllerException}.
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
 * As an example consider accessing a RAES encrypted ZIP file:
 * With the default driver configuration of the module TrueZIP ZIP.RAES,
 * whenever a ZIP.RAES file gets mounted, the user is prompted for a password.
 * If the user cancels the password prompting dialog, then an appropriate
 * exception gets thrown.
 * The target archive controller would then catch this exception and flag the
 * archive file as a false positive by wrapping this exception in a
 * {@link FsFalsePositiveException}.
 * This class would then catch this false positive exception and try to resolve
 * the issue by using the parent file system controller.
 * Failing that, the initial exception would get rethrown in order to signal
 * to the caller that the user had cancelled password prompting.
 *
 * @see    FsFalsePositiveException
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class FsFalsePositiveController
extends FsDecoratingController<FsModel, FsController<?>> {

    private volatile State state = new TryChild();

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

    @Nullable <T> T call(   final IOOperation<T> operation,
                            final FsEntryName name)
    throws IOException {
        final State state = this.state;
        try {
            return state.call(operation, name);
        } catch (final FsPersistentFalsePositiveException ex) {
            assert state instanceof TryChild;
            return (this.state = new UseParent(ex)).call(operation, name);
        } catch (final FsFalsePositiveException ex) {
            assert state instanceof TryChild;
            return new UseParent(ex).call(operation, name);
        }
    }

    @Override
    public FsController<?> getParent() {
        final FsController<?> parent = this.parent;
        return null != parent ? parent : (this.parent = delegate.getParent());
    }

    FsEntryName resolveParent(FsEntryName name) {
        return getPath().resolve(name).getEntryName();
    }

    private FsPath getPath() {
        final FsPath path = this.path;
        return null != path ? path : (this.path = getMountPoint().getPath());
    }

    @Override
    public boolean isReadOnly() throws IOException {
        return call(new IsReadOnly(), ROOT);
    }

    private static final class IsReadOnly implements IOOperation<Boolean> {
        @Override
        public Boolean call(final FsController<?> controller,
                            final FsEntryName name)
        throws IOException {
            return controller.isReadOnly();
        }
    } // IsReadOnly
    
    @Override
    public FsEntry getEntry(final FsEntryName name) throws IOException {
        return call(new GetEntry(), name);
    }

    private static final class GetEntry implements IOOperation<FsEntry> {
        @Override
        public FsEntry call(final FsController<?> controller,
                            final FsEntryName name)
        throws IOException {
            return controller.getEntry(name);
        }
    } // GetEntry

    @Override
    public boolean isReadable(final FsEntryName name) throws IOException {
        return call(new IsReadable(), name);
    }

    private static final class IsReadable implements IOOperation<Boolean> {
        @Override
        public Boolean call(final FsController<?> controller,
                            final FsEntryName name)
        throws IOException {
            return controller.isReadable(name);
        }
    } // IsReadable
    
    @Override
    public boolean isWritable(final FsEntryName name) throws IOException {
        return call(new IsWritable(), name);
    }

    private static final class IsWritable implements IOOperation<Boolean> {
        @Override
        public Boolean call(final FsController<?> controller,
                            final FsEntryName name)
        throws IOException {
            return controller.isWritable(name);
        }
    } // IsWritable

    @Override
    public boolean isExecutable(final FsEntryName name) throws IOException {
        return call(new IsExecutable(), name);
    }

    private static final class IsExecutable implements IOOperation<Boolean> {
        @Override
        public Boolean call(final FsController<?> controller,
                            final FsEntryName name)
        throws IOException {
            return controller.isExecutable(name);
        }
    } // IsWritable
    
    @Override
    public void setReadOnly(final FsEntryName name) throws IOException {
        call(new SetReadOnly(), name);
    }

    private static final class SetReadOnly implements IOOperation<Void> {
        @Override
        public Void call(   final FsController<?> controller,
                            final FsEntryName name)
        throws IOException {
            controller.setReadOnly(name);
            return null;
        }
    } // SetReadOnly
    
    @Override
    public boolean setTime(
            final FsEntryName name,
            final Map<Access, Long> times,
            final BitField<FsOutputOption> options)
    throws IOException {
        final class SetTime implements IOOperation<Boolean> {
            @Override
            public Boolean call(final FsController<?> controller,
                                final FsEntryName name)
            throws IOException {
                return controller.setTime(name, times, options);
            }
        } // SetTime

        return call(new SetTime(), name);
    }

    @Override
    public boolean setTime(
            final FsEntryName name,
            final BitField<Access> types,
            final long value,
            final BitField<FsOutputOption> options)
    throws IOException {
        final class SetTime implements IOOperation<Boolean> {
            @Override
            public Boolean call(final FsController<?> controller,
                                final FsEntryName name)
            throws IOException {
                return controller.setTime(name, types, value, options);
            }
        } // SetTime

        return call(new SetTime(), name);
    }

    @Override
    public InputSocket<?> getInputSocket(
            final FsEntryName name,
            final BitField<FsInputOption> options) {
        @NotThreadSafe
        final class Input extends InputSocket<Entry> {
            @CheckForNull FsController<?> lastController;
            @Nullable InputSocket<? extends Entry> delegate;

            InputSocket<?> getBoundDelegate(final FsController<?> controller,
                                            final FsEntryName name) {
                return (lastController == controller
                        ? delegate
                        : (delegate = (lastController = controller)
                            .getInputSocket(name, options)))
                        .bind(this);
            }

            @Override
            public Entry getLocalTarget() throws IOException {                
                return call(new GetLocalTarget(), name);
            }

            final class GetLocalTarget
            implements IOOperation<Entry> {
                @Override
                public Entry call(
                        final FsController<?> controller,
                        final FsEntryName name)
                throws IOException {
                    return getBoundDelegate(controller, name)
                            .getLocalTarget();
                }
            } // GetLocalTarget

            @Override
            public ReadOnlyFile newReadOnlyFile() throws IOException {
                return call(new NewReadOnlyFile(), name);
            }

            final class NewReadOnlyFile
            implements IOOperation<ReadOnlyFile> {
                @Override
                public ReadOnlyFile call(
                        final FsController<?> controller,
                        final FsEntryName name)
                throws IOException {
                    return getBoundDelegate(controller, name)
                            .newReadOnlyFile();
                }
            } // NewReadOnlyFile

            @Override
            public SeekableByteChannel newSeekableByteChannel()
            throws IOException {
                return call(new NewSeekableByteChannel(), name);
            }

            final class NewSeekableByteChannel
            implements IOOperation<SeekableByteChannel> {
                @Override
                public SeekableByteChannel call(
                        final FsController<?> controller,
                        final FsEntryName name)
                throws IOException {
                    return getBoundDelegate(controller, name)
                            .newSeekableByteChannel();
                }
            } // NewSeekableByteChannel

            @Override
            public InputStream newInputStream() throws IOException {
                return call(new NewInputStream(), name);
            }

            final class NewInputStream
            implements IOOperation<InputStream> {
                @Override
                public InputStream call(
                        final FsController<?> controller,
                        final FsEntryName name)
                throws IOException {
                    return getBoundDelegate(controller, name)
                            .newInputStream();
                }
            } // NewInputStream
        } // Input

        return new Input();
    }

    @Override
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE")
    public OutputSocket<?> getOutputSocket(
            final FsEntryName name,
            final BitField<FsOutputOption> options,
            final @CheckForNull Entry template) {
        @NotThreadSafe
        final class Output extends OutputSocket<Entry> {
            @CheckForNull FsController<?> lastController;
            @Nullable OutputSocket<? extends Entry> delegate;

            OutputSocket<?> getBoundDelegate(final FsController<?> controller,
                                             final FsEntryName name) {
                return (lastController == controller
                        ? delegate
                        : (delegate = (lastController = controller)
                            .getOutputSocket(name, options, template)))
                        .bind(this);
            }

            @Override
            public Entry getLocalTarget() throws IOException {                
                return call(new GetLocalTarget(), name);
            }

            final class GetLocalTarget
            implements IOOperation<Entry> {
                @Override
                public Entry call(
                        final FsController<?> controller,
                        final FsEntryName name)
                throws IOException {
                    return getBoundDelegate(controller, name)
                            .getLocalTarget();
                }
            } // GetLocalTarget

            @Override
            public SeekableByteChannel newSeekableByteChannel()
            throws IOException {
                return call(new NewSeekableByteChannel(), name);
            }

            final class NewSeekableByteChannel
            implements IOOperation<SeekableByteChannel> {
                @Override
                public SeekableByteChannel call(
                        final FsController<?> controller,
                        final FsEntryName name)
                throws IOException {
                    return getBoundDelegate(controller, name)
                            .newSeekableByteChannel();
                }
            } // NewSeekableByteChannel

            @Override
            public OutputStream newOutputStream() throws IOException {
                return call(new NewOutputStream(), name);
            }

            final class NewOutputStream
            implements IOOperation<OutputStream> {
                @Override
                public OutputStream call(
                        final FsController<?> controller,
                        final FsEntryName name)
                throws IOException {
                    return getBoundDelegate(controller, name)
                            .newOutputStream();
                }
            } // NewOutputStream
        } // Output

        return new Output();
    }

    @Override
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE")
    public void mknod(
            final FsEntryName name,
            final Type type,
            final BitField<FsOutputOption> options,
            final @CheckForNull Entry template)
    throws IOException {
        final class Mknod implements IOOperation<Void> {
            @Override
            public Void call(final FsController<?> controller,
                             final FsEntryName name)
            throws IOException {
                controller.mknod(name, type, options, template);
                return null;
            }
        } // Mknod

        call(new Mknod(), name);
    }

    @Override
    public void unlink(
            final FsEntryName name,
            final BitField<FsOutputOption> options)
    throws IOException {
        final class Unlink implements IOOperation<Void> {
            @Override
            public Void call(final FsController<?> controller,
                             final FsEntryName name)
            throws IOException {
                controller.unlink(name, options);
                if (name.isRoot()) {
                    assert controller == delegate;
                    // We have successfully removed the virtual root directory
                    // of a federated file system, i.e. an archive file.
                    // Now unlink the target archive file from the parent file
                    // system.
                    // Note that this makes an unlink operation NOT atomic
                    // because the lock for the parent file system is not
                    // acquired!
                    getParent().unlink(resolveParent(name), options);
                }
                return null;
            }
        } // Unlink

        if (name.isRoot())
            unlinkRoot(new Unlink());
        else
            call(new Unlink(), name);
    }

    private void unlinkRoot(final IOOperation<Void> operation)
    throws IOException {
        try {
            (state = new TryChild()).call(operation, ROOT);
        } catch (final FsFalsePositiveException ex) {
            new UseParent(ex).call(operation, ROOT);
        }
    }

    @Override
    public <X extends IOException> void
    sync(   final BitField<FsSyncOption> options,
            final ExceptionHandler<? super FsSyncException, X> handler)
    throws IOException {
        try {
            delegate.sync(options, handler);
        } catch (FsFalsePositiveException ex) {
            throw new AssertionError(ex);
        }
        state = new TryChild();
    }

    private interface IOOperation<T> {
        @Nullable T call(FsController<?> controller, FsEntryName name)
        throws IOException;
    } // IOOperation

    private interface State {
        @Nullable <T> T call(IOOperation<T> operation, FsEntryName name)
        throws IOException;
    } // State

    @Immutable
    private final class TryChild implements State {
        @Override
        public <T> T call(  final IOOperation<T> operation,
                            final FsEntryName name)
        throws IOException {
            return operation.call(delegate, name);
        }
    } // TryChild

    @Immutable
    private final class UseParent implements State {
        final IOException originalCause;

        UseParent(final FsFalsePositiveException ex) {
            this.originalCause = ex.getCause();
        }

        @Override
        public <T> T call(  final IOOperation<T> operation,
                            final FsEntryName name)
        throws IOException {
            try {
                return operation.call(getParent(), resolveParent(name));
            } catch (final FsControllerException ex) {
                assert !(ex instanceof FsFalsePositiveException);
                throw ex;
            } catch (final IOException ignored) {
                throw originalCause;
            }
        }
    } // UseParent
}
