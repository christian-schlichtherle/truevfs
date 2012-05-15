/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.kernel.se;

import de.schlichtherle.truezip.kernel.ControlFlowException;
import de.schlichtherle.truezip.kernel.FalsePositiveArchiveException;
import de.schlichtherle.truezip.kernel.NeedsLockRetryException;
import de.schlichtherle.truezip.kernel.PersistentFalsePositiveArchiveException;
import static de.truezip.kernel.FsEntryName.ROOT;
import de.truezip.kernel.*;
import de.truezip.kernel.cio.Entry.Access;
import de.truezip.kernel.cio.Entry.Type;
import de.truezip.kernel.cio.*;
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

    @Override
    public FsController<? extends FsModel> getParent() {
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

    private interface Operation<V> {
        @Nullable V apply(FsController<?> controller, FsEntryName name)
        throws IOException;
    } // IOOperation

    private interface State {
        @Nullable <V> V apply(FsEntryName name, Operation<V> operation)
        throws IOException;
    } // State

    @Immutable
    private final class TryChild implements State {
        @Override
        public <V> V apply(FsEntryName name, Operation<V> operation)
        throws IOException {
            return operation.apply(controller, name);
        }
    } // TryChild

    @Immutable
    private final class UseParent implements State {
        final IOException originalCause;

        UseParent(final FalsePositiveArchiveException ex) {
            this.originalCause = ex.getCause();
        }

        @Override
        public <V> V apply(
                final FsEntryName name,
                final Operation<V> operation)
        throws IOException {
            try {
                return operation.apply(getParent(), parent(name));
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

    @Nullable <V> V apply(
            final FsEntryName name,
            final Operation<V> operation)
    throws IOException {
        final State state = this.state;
        try {
            return state.apply(name, operation);
        } catch (final PersistentFalsePositiveArchiveException ex) {
            assert state instanceof TryChild;
            return (this.state = new UseParent(ex)).apply(name, operation);
        } catch (final FalsePositiveArchiveException ex) {
            assert state instanceof TryChild;
            return new UseParent(ex).apply(name, operation);
        }
    }

    @Override
    public @Nullable FsEntry stat(
            final BitField<FsAccessOption> options,
            final FsEntryName name)
    throws IOException {
        class Stat implements Operation<FsEntry> {
            @Override
            public FsEntry apply(FsController<?> c, FsEntryName n)
            throws IOException {
                return c.stat(options, n);
            }
        }
        return apply(name, new Stat());
    }

    @Override
    public void checkAccess(
            final BitField<FsAccessOption> options,
            final FsEntryName name,
            final BitField<Access> types)
    throws IOException {
        class CheckAccess implements Operation<Void> {
            @Override
            public Void apply(FsController<?> c, FsEntryName n)
            throws IOException {
                c.checkAccess(options, n, types);
                return null;
            }
        }
        apply(name, new CheckAccess());
    }

    @Override
    public void setReadOnly(final FsEntryName name) throws IOException {
        class SetReadOnly implements Operation<Void> {
            @Override
            public Void apply(FsController<?> c, FsEntryName n)
            throws IOException {
                c.setReadOnly(n);
                return null;
            }
        }
        apply(name, new SetReadOnly());
    }
    
    @Override
    public boolean setTime(
            final BitField<FsAccessOption> options,
            final FsEntryName name,
            final Map<Access, Long> times)
    throws IOException {
        class SetTime implements Operation<Boolean> {
            @Override
            public Boolean apply(FsController<?> c, FsEntryName n)
            throws IOException {
                return c.setTime(options, n, times);
            }
        }
        return apply(name, new SetTime());
    }

    @Override
    public boolean setTime(
            final BitField<FsAccessOption> options,
            final FsEntryName name,
            final BitField<Access> types,
            final long value)
    throws IOException {
        class SetTime implements Operation<Boolean> {
            @Override
            public Boolean apply(FsController<?> c, FsEntryName n)
            throws IOException {
                return c.setTime(options, n, types, value);
            }
        }
        return apply(name, new SetTime());
    }

    @Override
    public InputSocket<? extends Entry> input(
            final BitField<FsAccessOption> options,
            final FsEntryName name) {
        @NotThreadSafe
        class Input extends AbstractInputSocket<Entry> {
            @CheckForNull FsController<?> last;
            @Nullable InputSocket<?> socket;

            InputSocket<?> getBoundSocket(FsController<?> c, FsEntryName n) {
                return (last == c
                        ? socket
                        : (socket = (last = c).input(options, n))).bind(this);
            }

            @Override
            public Entry localTarget() throws IOException {                
                class GetLocalTarget implements Operation<Entry> {
                    @Override
                    public Entry apply(FsController<?> c, FsEntryName n)
                    throws IOException {
                        return getBoundSocket(c, n).localTarget();
                    }
                }
                return apply(name, new GetLocalTarget());
            }

            @Override
            public InputStream stream() throws IOException {
                class NewStream implements Operation<InputStream> {
                    @Override
                    public InputStream apply(FsController<?> c, FsEntryName n)
                    throws IOException {
                        return getBoundSocket(c, n).stream();
                    }
                }
                return apply(name, new NewStream());
            }

            @Override
            public SeekableByteChannel channel() throws IOException {
                class NewChannel implements Operation<SeekableByteChannel> {
                    @Override
                    public SeekableByteChannel apply(FsController<?> c, FsEntryName n)
                    throws IOException {
                        return getBoundSocket(c, n).channel();
                    }
                }
                return apply(name, new NewChannel());
            }

        } // Input
        return new Input();
    }

    @Override
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE")
    public OutputSocket<? extends Entry> output(
            final BitField<FsAccessOption> options,
            final FsEntryName name,
            final @CheckForNull Entry template) {
        @NotThreadSafe
        class Output extends AbstractOutputSocket<Entry> {
            @CheckForNull FsController<?> last;
            @Nullable OutputSocket<?> socket;

            OutputSocket<?> getBoundSocket(FsController<?> c, FsEntryName n) {
                return (last == c
                        ? socket
                        : (socket = (last = c).output(options, n, template))).bind(this);
            }

            @Override
            public Entry localTarget() throws IOException {                
                class GetLocalTarget implements Operation<Entry> {
                    @Override
                    public Entry apply(FsController<?> c, FsEntryName n)
                    throws IOException {
                        return getBoundSocket(c, n).localTarget();
                    }
                }
                return apply(name, new GetLocalTarget());
            }

            @Override
            public OutputStream stream() throws IOException {
                class NewStream implements Operation<OutputStream> {
                    @Override
                    public OutputStream apply(FsController<?> c, FsEntryName n)
                    throws IOException {
                        return getBoundSocket(c, n).stream();
                    }
                }
                return apply(name, new NewStream());
            }

            @Override
            public SeekableByteChannel channel() throws IOException {
                class NewChannel implements Operation<SeekableByteChannel> {
                    @Override
                    public SeekableByteChannel apply(FsController<?> c, FsEntryName n)
                    throws IOException {
                        return getBoundSocket(c, n).channel();
                    }
                }
                return apply(name, new NewChannel());
            }
        } // Output
        return new Output();
    }

    @Override
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE")
    public void mknod(
            final BitField<FsAccessOption> options,
            final FsEntryName name,
            final Type type,
            final @CheckForNull Entry template)
    throws IOException {
        class Mknod implements Operation<Void> {
            @Override
            public Void apply(FsController<?> c, FsEntryName n)
            throws IOException {
                c.mknod(options, n, type, template);
                return null;
            }
        }
        apply(name, new Mknod());
    }

    @Override
    public void unlink(
            final BitField<FsAccessOption> options,
            final FsEntryName name)
    throws IOException {
        class Unlink implements Operation<Void> {
            @Override
            public Void apply(FsController<?> c, FsEntryName n)
            throws IOException {
                c.unlink(options, n); // repeatable for root entry
                if (n.isRoot()) {
                    assert c == FalsePositiveArchiveController.this.controller;
                    // Unlink target archive file from parent file system.
                    // This operation isn't lock protected, so it's not atomic!
                    getParent().unlink(options, parent(n));
                }
                return null;
            }
        }

        final Operation<Void> operation = new Unlink();
        if (name.isRoot()) {
            // HC SUNT DRACONES!
            final State tryChild = new TryChild();
            try {
                tryChild.apply(ROOT, operation);
            } catch (final FalsePositiveArchiveException ex) {
                new UseParent(ex).apply(ROOT, operation);
            }
            this.state = tryChild;
        } else {
            apply(name, operation);
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
}
