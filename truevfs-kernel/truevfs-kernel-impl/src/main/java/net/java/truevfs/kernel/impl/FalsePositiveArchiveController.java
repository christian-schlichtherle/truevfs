/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl;

import bali.Cache;
import net.java.truecommons.cio.*;
import net.java.truecommons.shed.BitField;
import net.java.truecommons.shed.ControlFlowException;
import net.java.truevfs.kernel.spec.*;

import javax.annotation.CheckForNull;
import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.Map;

import static net.java.truevfs.kernel.spec.FsNodeName.ROOT;

/**
 * Implements a chain of responsibility for resolving
 * {@link FalsePositiveArchiveException}s which may get thrown by its decorated file system controller.
 * <p>
 * This controller is a barrier for {@code FalsePositiveArchiveException}s:
 * Whenever the decorated controller chain throws a {@code FalsePositiveArchiveException}, the file system operation is
 * routed to the controller of the parent file system in order to continue the operation.
 * If this fails with an {@link IOException}, then the {@code IOException} which is associated as the original cause of
 * the initial {@code FalsePositiveArchiveException} gets rethrown.
 * <p>
 * This algorithm effectively achieves the following objectives:
 * <ol>
 * <li>False positive archive files get resolved correctly by accessing them as entities of the parent file system.
 * <li>If the file system driver for the parent file system throws another exception, then it gets discarded and the
 *     exception initially thrown by the file system driver for the false positive archive file takes its place in order
 *     to provide the caller with a good indication of what went wrong in the first place.
 * <li>Non-{@code IOException}s are excluded from this masquerade in order to support resolving them by a more competent
 *     caller.
 *     This is required to make {@link net.java.truecommons.shed.ControlFlowException}s work as designed.
 * </ol>
 * <p>
 * As an example consider accessing a RAES encrypted ZIP file:
 * With the default driver configuration of the module TrueVFS ZIP.RAES, whenever a ZIP.RAES file gets mounted, the user
 * is prompted for a password.
 * If the user cancels the password prompting dialog, then an appropriate exception gets thrown.
 * The target archive controller would then catch this exception and flag the archive file as a false positive by
 * wrapping this exception in a {@code FalsePositiveArchiveException}.
 * This class would then catch this false positive exception and try to resolve the issue by using the parent file
 * system controller.
 * Failing that, the initial exception would get rethrown in order to signal to the caller that the user had cancelled
 * password prompting.
 *
 * @author Christian Schlichtherle
 * @see FalsePositiveArchiveException
 */
@ThreadSafe
abstract class FalsePositiveArchiveController implements FsDelegatingController {

    private final State tryChild = new State() {

        @Override
        public <T> T apply(FsNodeName name, Op<T> op) throws IOException {
            return op.call(getController(), name);
        }
    };

    private volatile State state = tryChild;

    @Cache
    public FsNodePath getPath() {
        return getMountPoint().getPath();
    }

    private FsNodeName parent(FsNodeName name) {
        return getPath().resolve(name).getNodeName();
    }

    @CheckForNull
    @Override
    public FsNode node(BitField<FsAccessOption> options, FsNodeName name) throws IOException {
        return apply(name, (c, n) -> c.node(options, n));
    }

    @Override
    public void checkAccess(
            BitField<FsAccessOption> options,
            FsNodeName name,
            BitField<Entry.Access> types
    ) throws IOException {
        apply(name, (c, n) -> {
            c.checkAccess(options, n, types);
            return null;
        });
    }

    @Override
    public void setReadOnly(BitField<FsAccessOption> options, FsNodeName name) throws IOException {
        apply(name, (c, n) -> {
            c.setReadOnly(options, n);
            return null;
        });
    }

    @Override
    public boolean setTime(
            BitField<FsAccessOption> options,
            FsNodeName name,
            Map<Entry.Access, Long> times
    ) throws IOException {
        return apply(name, (c, n) -> c.setTime(options, n, times));
    }

    @Override
    public boolean setTime(
            BitField<FsAccessOption> options,
            FsNodeName name,
            BitField<Entry.Access> types,
            long value
    ) throws IOException {
        return apply(name, (c, n) -> c.setTime(options, n, types, value));
    }

    @Override
    public InputSocket<? extends Entry> input(BitField<FsAccessOption> options, FsNodeName name) {
        return new AbstractInputSocket<Entry>() {

            FsController last;
            InputSocket<? extends Entry> socket;

            InputSocket<? extends Entry> socket(FsController c, FsNodeName n) {
                if (last != c) {
                    last = c;
                    socket = c.input(options, n);
                }
                return socket;
            }

            @Override
            public Entry target() throws IOException {
                return apply(name, (c, n) -> socket(c, n).target());
            }

            @Override
            public InputStream stream(OutputSocket<? extends Entry> peer) throws IOException {
                return apply(name, (c, n) -> socket(c, n).stream(peer));
            }

            @Override
            public SeekableByteChannel channel(OutputSocket<? extends Entry> peer) throws IOException {
                return apply(name, (c, n) -> socket(c, n).channel(peer));
            }
        };
    }

    @Override
    public OutputSocket<? extends Entry> output(
            BitField<FsAccessOption> options,
            FsNodeName name,
            @CheckForNull Entry template
    ) {
        return new AbstractOutputSocket<Entry>() {

            FsController last;
            OutputSocket<? extends Entry> socket;

            OutputSocket<? extends Entry> socket(FsController c, FsNodeName n) {
                if (last != c) {
                    last = c;
                    socket = c.output(options, n, template);
                }
                return socket;
            }

            @Override
            public Entry target() throws IOException {
                return apply(name, (c, n) -> socket(c, n).target());
            }

            @Override
            public OutputStream stream(InputSocket<? extends Entry> peer) throws IOException {
                return apply(name, (c, n) -> socket(c, n).stream(peer));
            }

            @Override
            public SeekableByteChannel channel(InputSocket<? extends Entry> peer) throws IOException {
                return apply(name, (c, n) -> socket(c, n).channel(peer));
            }
        };
    }

    @Override
    public void make(
            BitField<FsAccessOption> options,
            FsNodeName name,
            Entry.Type type,
            @CheckForNull Entry template
    ) throws IOException {
        apply(name, (c, n) -> {
            c.make(options, n, type, template);
            return null;
        });
    }

    @Override
    public void unlink(final BitField<FsAccessOption> options, final FsNodeName name) throws IOException {
        final Op<Void> op = (c, n) -> {
            c.unlink(options, n);
            if (n.isRoot()) {
                assert c == getController();
                // Unlink target archive file from parent file system.
                // This operation isn't lock protected, so it's not atomic!
                getParent().unlink(options, parent(n));
            }
            return null;
        };
        if (name.isRoot()) {
            // HC SVNT DRACONES!
            try {
                tryChild.apply(ROOT, op);
            } catch (FalsePositiveArchiveException e) {
                new UseParent(e).apply(ROOT, op);
            }
            state = tryChild;
        } else {
            apply(name, op);
        }
    }

    @Override
    public void sync(final BitField<FsSyncOption> options) throws FsSyncException {
        // HC SVNT DRACONES!
        try {
            getController().sync(options);
        } catch (final FsSyncException | ControlFlowException e) {
            assert state == tryChild;
            throw e;
        }
        state = tryChild;
    }

    private <T> T apply(final FsNodeName name, final Op<T> op) throws IOException {
        State state = this.state;
        try {
            return state.apply(name, op);
        } catch (final FalsePositiveArchiveException e) {
            assert state == tryChild;
            state = new UseParent(e);
            if (e instanceof PersistentFalsePositiveArchiveException) {
                this.state = state;
            }
            return state.apply(name, op);
        }
    }

    @FunctionalInterface
    private interface Op<T> {

        T call(FsController c, FsNodeName n) throws IOException;
    }

    @FunctionalInterface
    private interface State {

        <T> T apply(FsNodeName name, Op<T> op) throws IOException;
    }

    private final class UseParent implements State {

        private final FalsePositiveArchiveException original;
        private final IOException originalCause;

        private UseParent(final FalsePositiveArchiveException original) {
            this.original = original;
            this.originalCause = original.getCause();
        }

        @Override
        public <T> T apply(final FsNodeName name, final Op<T> op) throws IOException {
            try {
                return op.call(getParent(), parent(name));
            } catch (FalsePositiveArchiveException e) {
                throw new AssertionError(e);
            } catch (final IOException e) {
                if (originalCause != e) {
                    originalCause.addSuppressed(e);
                }
                throw originalCause;
            } catch (final Throwable e) {
                assert !(e instanceof ControlFlowException) || e instanceof NeedsLockRetryException;
                e.addSuppressed(original); // provide full context
                throw e;
            }
        }
    }
}
