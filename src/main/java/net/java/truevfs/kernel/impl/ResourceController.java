/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl;

import lombok.val;
import net.java.truecommons.cio.*;
import net.java.truecommons.io.DecoratingInputStream;
import net.java.truecommons.io.DecoratingOutputStream;
import net.java.truecommons.io.DecoratingSeekableChannel;
import net.java.truecommons.shed.BitField;
import net.java.truecommons.shed.ControlFlowException;
import net.java.truecommons.shed.ExceptionHandler;
import net.java.truevfs.kernel.spec.*;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.Optional;

import static net.java.truevfs.kernel.spec.FsSyncOption.FORCE_CLOSE_IO;
import static net.java.truevfs.kernel.spec.FsSyncOption.WAIT_CLOSE_IO;

/**
 * Accounts input and output resources returned by its decorated controller.
 *
 * @author Christian Schlichtherle
 * @see ResourceAccountant
 */
abstract class ResourceController<E extends FsArchiveEntry> implements DelegatingArchiveController<E> {

    private static final int waitTimeoutMillis = LockingStrategy.acquireTimeoutMillis;

    private final ResourceAccountant accountant = new ResourceAccountant(writeLock());

    @Override
    public InputSocket<? extends Entry> input(BitField<FsAccessOption> options, FsNodeName name) {
        return new DelegatingInputSocket<Entry>() {

            final InputSocket<? extends Entry> socket = getController().input(options, name);

            @Override
            protected InputSocket<? extends Entry> socket() throws IOException {
                return socket;
            }

            @Override
            public InputStream stream(OutputSocket<? extends Entry> peer) throws IOException {
                return new ResourceInputStream(socket.stream(peer));
            }

            @Override
            public SeekableByteChannel channel(OutputSocket<? extends Entry> peer) throws IOException {
                return new ResourceSeekableChannel(socket.channel(peer));
            }
        };
    }

    @Override
    public OutputSocket<? extends Entry> output(BitField<FsAccessOption> options, FsNodeName name, Optional<Entry> template) {
        return new DelegatingOutputSocket<Entry>() {

            final OutputSocket<? extends Entry> socket = getController().output(options, name, template);

            @Override
            protected OutputSocket<? extends Entry> socket() throws IOException {
                return socket;
            }

            @Override
            public OutputStream stream(InputSocket<? extends Entry> peer) throws IOException {
                return new ResourceOutputStream(socket.stream(peer));
            }

            @Override
            public SeekableByteChannel channel(InputSocket<? extends Entry> peer) throws IOException {
                return new ResourceSeekableChannel(socket.channel(peer));
            }
        };
    }

    @Override
    public void sync(final BitField<FsSyncOption> options) throws FsSyncException {
        assert writeLockedByCurrentThread();
        assert !readLockedByCurrentThread();

        // HC SVNT DRACONES!
        val beforeWait = accountant.resources();
        if (0 == beforeWait.getTotal()) {
            getController().sync(options);
            return;
        }

        val builder = new FsSyncExceptionBuilder();
        try {
            if (0 != beforeWait.getLocal() && !options.get(FORCE_CLOSE_IO)) {
                throw new FsOpenResourceException(beforeWait.getLocal(), beforeWait.getTotal());
            }
            accountant.awaitClosingOfOtherThreadsResources(options.get(WAIT_CLOSE_IO) ? 0 : waitTimeoutMillis);
            val afterWait = accountant.resources();
            if (0 != afterWait.getTotal()) {
                throw new FsOpenResourceException(afterWait.getLocal(), afterWait.getTotal());
            }
        } catch (final FsOpenResourceException e) {
            if (!options.get(FORCE_CLOSE_IO)) {
                throw builder.fail(new FsSyncException(getMountPoint(), e));
            }
            builder.warn(new FsSyncWarningException(getMountPoint(), e));
        }
        closeResources(builder);
        if (beforeWait.isNeedsWaiting()) {
            // awaitClosingOfOtherThreadsResources(*) has temporarily released the write lock, so the state of the
            // virtual file system may have completely changed and thus we need to restart the sync operation unless an
            // exception occured.
            builder.check();
            throw NeedsSyncException.apply();
        }
        try {
            getController().sync(options);
        } catch (FsSyncException e) {
            throw builder.fail(e);
        }
        builder.check();
    }

    /**
     * Closes and disconnects all entry streams of the output and input archive.
     *
     * @param builder the exception handling strategy.
     */
    private void closeResources(final FsSyncExceptionBuilder builder) {
        accountant.closeAllResources(new ExceptionHandler<IOException, RuntimeException>() {

            @Override
            public RuntimeException fail(IOException input) {
                throw new AssertionError();
            }

            @Override
            public void warn(IOException input) throws RuntimeException {
                builder.warn(new FsSyncWarningException(getMountPoint(), input));
            }
        });
    }

    private final class ResourceInputStream extends DecoratingInputStream {

        final ResourceCloseable closeable = new ResourceCloseable(in);

        ResourceInputStream(InputStream in) {
            super(in);
        }

        @Override
        public void close() throws IOException {
            closeable.close();
        }
    }

    private final class ResourceOutputStream extends DecoratingOutputStream {

        final ResourceCloseable closeable = new ResourceCloseable(out);

        ResourceOutputStream(OutputStream out) {
            super(out);
        }

        @Override
        public void close() throws IOException {
            closeable.close();
        }
    }

    private final class ResourceSeekableChannel extends DecoratingSeekableChannel {

        final ResourceCloseable closeable = new ResourceCloseable(channel);

        ResourceSeekableChannel(SeekableByteChannel channel) {
            super(channel);
        }

        @Override
        public void close() throws IOException {
            closeable.close();
        }
    }

    private final class ResourceCloseable implements Closeable {

        final Closeable closeable;

        ResourceCloseable(final Closeable closeable) {
            this.closeable = closeable;
            accountant.startAccountingFor(this);
        }

        /**
         * Close()s this resource and finally stops accounting for it unless a {@link ControlFlowException} is thrown.
         *
         * @see <a href="http://java.net/jira/browse/TRUEZIP-279">Issue TRUEZIP-279</a>
         */
        @Override
        public void close() throws IOException {
            boolean cfe = false;
            try {
                closeable.close();
            } catch (final ControlFlowException e) {
                cfe = true;
                throw e;
            } finally {
                if (!cfe) {
                    accountant.stopAccountingFor(this);
                }
            }
        }
    }
}
