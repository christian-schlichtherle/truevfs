/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.kernel;

import static de.truezip.kernel.FsEntryName.ROOT;
import de.truezip.kernel.*;
import de.truezip.kernel.cio.Entry;
import de.truezip.kernel.cio.Entry.Access;
import de.truezip.kernel.cio.Entry.Type;
import de.truezip.kernel.cio.InputSocket;
import de.truezip.kernel.cio.OutputSocket;
import de.truezip.kernel.util.BitField;
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
 * {@link FalsePositiveArchiveException}s which may get thrown by its decorated file
 * system controller.
 * <p>
 * This controller is a barrier for {@link FalsePositiveArchiveException}s:
 * Whenever the decorated controller chain throws a
 * {@code FalsePositiveArchiveException}, the file system operation is routed to the
 * controller of the parent file system in order to continue the operation.
 * If this fails with an {@link IOException}, then the {@code IOException}
 * which is associated as the original cause of the initial
 * {@code FalsePositiveArchiveException} gets rethrown.
 * <p>
 * This algorithm effectively achieves the following objectives:
 * <ol>
 * <li>False positive archive files get resolved correctly by accessing them as
 *     entities of the parent file system.
 * <li>If the file system driver for the parent file system throws another
 *     exception, then it gets discarded and the exception initially thrown by
 *     the file system driver for the false positive archive file takes its
 *     place in order to provide the caller with a good indication of what went
 *     wrong in the first place.
 * <li>Non-{@code IOException}s are excempt from this masquerade in order to
 *     support resolving them by a more competent caller.
 *     This is required to make {@link ControlFlowException}s work as designed.
 * </ol>
 * <p>
 * As an example consider accessing a RAES encrypted ZIP file:
 * With the default driver configuration of the module TrueZIP ZIP.RAES,
 * whenever a ZIP.RAES file gets mounted, the user is prompted for a password.
 * If the user cancels the password prompting dialog, then an appropriate
 * exception gets thrown.
 * The target archive controller would then catch this exception and flag the
 * archive file as a false positive by wrapping this exception in a
 * {@code FalsePositiveArchiveException}.
 * This class would then catch this false positive exception and try to resolve
 * the issue by using the parent file system controller.
 * Failing that, the initial exception would get rethrown in order to signal
 * to the caller that the user had cancelled password prompting.
 *
 * @see    FalsePositiveArchiveException
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class FalsePositiveArchiveController
extends FsDecoratingController<FsModel, FsController<?>> {

    // This may change when only a read-lock is held.
    private volatile State state = new TryChild();

    // These fields don't need to be volatile because reads and writes of
    // references are always atomic and the objects are the results of
    // idempotent functions.
    // See The Java Language Specification, Third Edition, section 17.7
    // "Non-atomic Treatment of double and long".
    private /*volatile*/ @CheckForNull FsController<?> parent;
    private /*volatile*/ @CheckForNull FsPath path;

    FalsePositiveArchiveController(final FsController<?> controller) {
        super(controller);
        assert null != super.getParent();
    }

    @Nullable <V> V call(   final IOOperation<V> operation,
                            final FsEntryName name)
    throws IOException {
        final State state = this.state;
        try {
            return state.call(operation, name);
        } catch (final PersistentFalsePositiveArchiveException ex) {
            assert state instanceof TryChild;
            return (this.state = new UseParent(ex)).call(operation, name);
        } catch (final FalsePositiveArchiveException ex) {
            assert state instanceof TryChild;
            return new UseParent(ex).call(operation, name);
        }
    }

    @Override
    public FsController<?> getParent() {
        final FsController<?> parent = this.parent;
        return null != parent ? parent : (this.parent = controller.getParent());
    }

    FsEntryName parent(FsEntryName name) {
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
                            final FsEntryName resolved)
        throws IOException {
            return controller.isReadOnly();
        }
    } // IsReadOnly
    
    @Override
    public @Nullable FsEntry stat(final FsEntryName name) throws IOException {
        return call(new GetEntry(), name);
    }

    private static final class GetEntry implements IOOperation<FsEntry> {
        @Override
        public @Nullable FsEntry call(  final FsController<?> controller,
                                        final FsEntryName resolved)
        throws IOException {
            return controller.stat(resolved);
        }
    } // GetEntry

    @Override
    public void checkAccess(
            final FsEntryName name,
            final BitField<FsAccessOption> options,
            final BitField<Access> types)
    throws IOException {
        final class CheckAccess implements IOOperation<Void> {
            @Override
            public Void call(   final FsController<?> controller,
                                final FsEntryName resolved)
            throws IOException {
                controller.checkAccess(resolved, options, types);
                return null;
            }
        } // CheckAccess

        call(new CheckAccess(), name);
    }

    @Override
    public void setReadOnly(final FsEntryName name) throws IOException {
        call(new SetReadOnly(), name);
    }

    private static final class SetReadOnly implements IOOperation<Void> {
        @Override
        public Void call(   final FsController<?> controller,
                            final FsEntryName resolved)
        throws IOException {
            controller.setReadOnly(resolved);
            return null;
        }
    } // SetReadOnly
    
    @Override
    public boolean setTime(
            final FsEntryName name, final BitField<FsAccessOption> options, final Map<Access, Long> times)
    throws IOException {
        final class SetTime implements IOOperation<Boolean> {
            @Override
            public Boolean call(final FsController<?> controller,
                                final FsEntryName resolved)
            throws IOException {
                return controller.setTime(resolved, options, times);
            }
        } // SetTime

        return call(new SetTime(), name);
    }

    @Override
    public boolean setTime(
            final FsEntryName name, final BitField<FsAccessOption> options, final BitField<Access> types, final long value)
    throws IOException {
        final class SetTime implements IOOperation<Boolean> {
            @Override
            public Boolean call(final FsController<?> controller,
                                final FsEntryName resolved)
            throws IOException {
                return controller.setTime(resolved, options, types, value);
            }
        } // SetTime

        return call(new SetTime(), name);
    }

    @Override
    public InputSocket<?> input(
            final FsEntryName name,
            final BitField<FsAccessOption> options) {
        @NotThreadSafe
        final class Input extends InputSocket<Entry> {
            @CheckForNull FsController<?> lastController;
            @Nullable InputSocket<?> socket;

            InputSocket<?> getBoundSocket(  final FsController<?> controller,
                                            final FsEntryName resolved) {
                return (lastController == controller
                        ? socket
                        : (socket = (lastController = controller)
                            .input(resolved, options)))
                        .bind(this);
            }

            @Override
            public Entry localTarget() throws IOException {                
                return call(new GetLocalTarget(), name);
            }

            final class GetLocalTarget implements IOOperation<Entry> {
                @Override
                public Entry call(
                        final FsController<?> controller,
                        final FsEntryName resolved)
                throws IOException {
                    return getBoundSocket(controller, resolved).localTarget();
                }
            } // GetLocalTarget

            @Override
            public InputStream stream() throws IOException {
                return call(new NewStream(), name);
            }

            final class NewStream implements IOOperation<InputStream> {
                @Override
                public InputStream call(
                        final FsController<?> controller,
                        final FsEntryName resolved)
                throws IOException {
                    return getBoundSocket(controller, resolved).stream();
                }
            } // NewStream

            @Override
            public SeekableByteChannel channel()
            throws IOException {
                return call(new NewChannel(), name);
            }

            final class NewChannel implements IOOperation<SeekableByteChannel> {
                @Override
                public SeekableByteChannel call(
                        final FsController<?> controller,
                        final FsEntryName resolved)
                throws IOException {
                    return getBoundSocket(controller, resolved).channel();
                }
            } // NewChannel
        } // Input

        return new Input();
    }

    @Override
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE")
    public OutputSocket<?> output(
            final FsEntryName name,
            final BitField<FsAccessOption> options,
            final @CheckForNull Entry template) {
        @NotThreadSafe
        final class Output extends OutputSocket<Entry> {
            @CheckForNull FsController<?> lastController;
            @Nullable OutputSocket<?> socket;

            OutputSocket<?> getBoundSocket( final FsController<?> controller,
                                            final FsEntryName resolved) {
                return (lastController == controller
                        ? socket
                        : (socket = (lastController = controller)
                            .output(resolved, options, template)))
                        .bind(this);
            }

            @Override
            public Entry localTarget() throws IOException {                
                return call(new GetLocalTarget(), name);
            }

            final class GetLocalTarget implements IOOperation<Entry> {
                @Override
                public Entry call(
                        final FsController<?> controller,
                        final FsEntryName resolved)
                throws IOException {
                    return getBoundSocket(controller, resolved).localTarget();
                }
            } // GetLocalTarget

            @Override
            public OutputStream stream() throws IOException {
                return call(new NewStream(), name);
            }

            final class NewStream implements IOOperation<OutputStream> {
                @Override
                public OutputStream call(
                        final FsController<?> controller,
                        final FsEntryName resolved)
                throws IOException {
                    return getBoundSocket(controller, resolved).stream();
                }
            } // NewStream

            @Override
            public SeekableByteChannel channel()
            throws IOException {
                return call(new NewChannel(), name);
            }

            final class NewChannel implements IOOperation<SeekableByteChannel> {
                @Override
                public SeekableByteChannel call(
                        final FsController<?> controller,
                        final FsEntryName resolved)
                throws IOException {
                    return getBoundSocket(controller, resolved).channel();
                }
            } // NewChannel
        } // Output

        return new Output();
    }

    @Override
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE")
    public void mknod(
            final FsEntryName name, final BitField<FsAccessOption> options, final Type type, @CheckForNull
    final Entry template)
    throws IOException {
        final class Mknod implements IOOperation<Void> {
            @Override
            public Void call(final FsController<?> controller,
                             final FsEntryName resolved)
            throws IOException {
                controller.mknod(resolved, options, type, template);
                return null;
            }
        } // Mknod

        call(new Mknod(), name);
    }

    @Override
    public void unlink(
            final FsEntryName name,
            final BitField<FsAccessOption> options)
    throws IOException {
        final class Unlink implements IOOperation<Void> {
            @Override
            public Void call(final FsController<?> controller,
                             final FsEntryName resolved)
            throws IOException {
                controller.unlink(resolved, options); // repeatable for root entry
                if (resolved.isRoot()) {
                    assert controller == FalsePositiveArchiveController.this.controller;
                    // Unlink target archive file from parent file system.
                    // This operation isn't lock protected, so it's not atomic!
                    getParent().unlink(parent(resolved), options);
                }
                return null;
            }
        } // Unlink

        final IOOperation<Void> operation = new Unlink();
        if (name.isRoot()) {
            // HC SUNT DRACONES!
            final State tryChild = new TryChild();
            try {
                tryChild.call(operation, ROOT);
            } catch (final FalsePositiveArchiveException ex) {
                new UseParent(ex).call(operation, ROOT);
            }
            this.state = tryChild;
        } else {
            call(operation, name);
        }
    }

    @Override
    public void sync(final BitField<FsSyncOption> options)
    throws FsSyncWarningException, FsSyncException {
        // HC SUNT DRACONES!
        try {
            controller.sync(options);
        } catch (final FsSyncException | ControlFlowException ex) {
            assert state instanceof TryChild;
            throw ex;
        }
        state = new TryChild();
    }

    private interface IOOperation<V> {
        @Nullable V call(FsController<?> controller, FsEntryName resolved)
        throws IOException;
    } // IOOperation

    private interface State {
        @Nullable <V> V call(IOOperation<V> operation, FsEntryName name)
        throws IOException;
    } // State

    @Immutable
    private final class TryChild implements State {
        @Override
        public <V> V call(  final IOOperation<V> operation,
                            final FsEntryName name)
        throws IOException {
            return operation.call(controller, name);
        }
    } // TryChild

    @Immutable
    private final class UseParent implements State {
        final IOException originalCause;

        UseParent(final FalsePositiveArchiveException ex) {
            this.originalCause = ex.getCause();
        }

        @Override
        public <V> V call(  final IOOperation<V> operation,
                            final FsEntryName name)
        throws IOException {
            try {
                return operation.call(getParent(), parent(name));
            } catch (final FalsePositiveArchiveException ex) {
                throw new AssertionError(ex);
            } catch (final ControlFlowException ex) {
                assert ex instanceof NeedsLockRetryException : ex;
                throw ex;
            } catch (final IOException ex) {
                if (originalCause != ex)
                    originalCause.addSuppressed(ex);
                throw originalCause;
            }
        }
    } // UseParent
}
