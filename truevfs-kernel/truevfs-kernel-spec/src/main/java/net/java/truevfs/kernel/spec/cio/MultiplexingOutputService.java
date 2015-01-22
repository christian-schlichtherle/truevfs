/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec.cio;

import edu.umd.cs.findbugs.annotations.CleanupObligation;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import edu.umd.cs.findbugs.annotations.DischargesObligation;
import net.java.truecommons.cio.*;
import net.java.truecommons.cio.Entry.Access;
import net.java.truecommons.io.DecoratingOutputStream;
import net.java.truecommons.io.InputException;
import net.java.truecommons.shed.CompoundIterator;
import net.java.truecommons.shed.ExceptionBuilder;
import net.java.truecommons.shed.PriorityExceptionBuilder;
import net.java.truecommons.shed.SuppressedExceptionBuilder;

import javax.annotation.CheckForNull;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.NotThreadSafe;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

import static net.java.truecommons.cio.Entry.ALL_ACCESS;
import static net.java.truecommons.cio.Entry.Size.DATA;
import static net.java.truecommons.cio.Entry.UNKNOWN;

/**
 * Decorates another output service to enable concurrent writing of multiple
 * entries to the decorated container.
 * Whenever an attempt is made to write more than one entry concurrently to
 * this container, all but the first entry is transparently redirected to an
 * I/O buffer.
 * Whenever a redirected entry is {@code close()}d then, another attempt is
 * made to copy the I/O buffer into the decorated container.
 * If this container is still busy with writing an entry to the decorated
 * container, then the copying is deferred until either another I/O buffer
 * gets {@code close()}d or this container gets {@code close()}d,
 * whatever happens first.
 * <p>
 * Note that this implies that {@code close()}ing an entry or this container
 * may fail with an {@link IOException}.
 *
 * @param  <E> the type of the mutable entries.
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public class MultiplexingOutputService<E extends MutableEntry>
extends DecoratingOutputService<E> {

    private final IoBufferPool pool;

    /**
     * The map of temporary archive entries which have not yet been written
     * to the output archive.
     */
    private final Map<String, BufferedEntryOutputStream>
            buffers = new LinkedHashMap<>();

    /** @see #isBusy */
    private boolean busy;

    /**
     * Constructs a new multiplexed output service.
     *
     * @param output the decorated output service.
     * @param pool the pool for buffering entry data.
     */
    public MultiplexingOutputService(
            final IoBufferPool pool,
            final @WillCloseWhenClosed OutputService<E> output) {
        super(output);
        this.pool = Objects.requireNonNull(pool);
    }

    @Override
    public int size() {
        return container.size() + buffers.size();
    }

    @Override
    public Iterator<E> iterator() {
        return new CompoundIterator<>(
                container.iterator(),
                new BufferedEntriesIterator());
    }

    private class BufferedEntriesIterator implements Iterator<E> {
        final Iterator<BufferedEntryOutputStream> i = buffers.values().iterator();

        @Override
        public boolean hasNext() {
            return i.hasNext();
        }

        @Override
        public E next() {
            return i.next().getTarget();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public @CheckForNull E entry(String name) {
        final E entry = container.entry(name);
        if (null != entry) return entry;
        final BufferedEntryOutputStream out = buffers.get(name);
        return null == out ? null : out.getTarget();
    }

    @Override
    public OutputSocket<E> output(final E local) {
        Objects.requireNonNull(local);
        final class Output extends DecoratingOutputSocket<E> {
            Output() { super(container.output(local)); }

            @Override
            public E target() { return local; }

            @Override
            public OutputStream stream(InputSocket<? extends Entry> peer)
            throws IOException {
                return isBusy() ? new BufferedEntryOutputStream(socket(), peer)
                                : new EntryOutputStream(socket().stream(peer));
            }
        } // Output
        return new Output();
    }

    /**
     * Returns whether the container output archive is busy writing an archive
     * entry or not.
     *
     * @return Whether the container output archive is busy writing an archive
     *         entry or not.
     */
    public boolean isBusy() {
        return busy;
    }

    @Override
    @DischargesObligation
    public void close() throws IOException {
        if (isBusy())
            throw new IOException("This multiplexing output service is still busy with writing a stream!");
        storeBuffers();
        assert buffers.isEmpty();
        container.close();
    }

    final void storeBuffers() throws IOException {
        if (isBusy()) return;
        final ExceptionBuilder<IOException, IOException> builder
                = new PriorityExceptionBuilder<>(IOExceptionComparator.INSTANCE);
        for (   final Iterator<BufferedEntryOutputStream> i = buffers.values().iterator();
                i.hasNext(); ) {
            final BufferedEntryOutputStream out = i.next();
            try {
                if (out.storeBuffer()) i.remove();
            } catch (InputException ex) {
                builder.warn(ex);
            } catch (IOException ex) {
                throw builder.fail(ex);
            }
        }
        builder.check();
    }

    private static final class IOExceptionComparator
    implements Comparator<IOException> {
        static final IOExceptionComparator INSTANCE = new IOExceptionComparator();

        @Override
        public int compare(IOException o1, IOException o2) {
            return    (o1 instanceof InputException ? 0 : 1)
                    - (o2 instanceof InputException ? 0 : 1);
        }
    } // IOExceptionComparator

    /** This entry output stream writes directly to this output service. */
    private final class EntryOutputStream extends DecoratingOutputStream {
        boolean closed;

        EntryOutputStream(final @WillCloseWhenClosed OutputStream out) {
            super(out);
            busy = true;
        }

        @Override
        @DischargesObligation
        public void close() throws IOException {
            if (!closed) {
                closed = true;
                busy = false;
                out.close();
            }
            storeBuffers();
        }
    } // EntryOutputStream

    /**
     * This entry output stream writes the archive entry to an
     * {@linkplain IoBuffer I/O buffer}.
     * When the stream gets closed, the I/O buffer is then copied to this
     * output service and finally deleted unless this output service is still busy.
     */
    @CleanupObligation
    private final class BufferedEntryOutputStream
    extends DecoratingOutputStream {
        final InputSocket<?> input;
        final OutputSocket<? extends E> output;
        final IoBuffer buffer;
        boolean closed;

        @CreatesObligation
        @SuppressWarnings("LeakingThisInConstructor")
        BufferedEntryOutputStream(
                final OutputSocket<? extends E> output,
                final @CheckForNull InputSocket<? extends Entry> input)
        throws IOException {
            // HC SVNT DRACONES!
            final E local = (this.output = output).target();
            final Entry _peer = null != input ? input.target() : null;
            final IoBuffer buffer = this.buffer = pool.allocate();
            final Entry peer = null != _peer ? _peer : buffer;
            final class InputProxy extends DecoratingInputSocket<Entry> {
                InputProxy() { super(buffer.input()); }

                @Override
                public Entry target() { return peer; }
            } // InputProxy
            try {
                this.input = new InputProxy();
                this.out = buffer.output().stream(null);
            } catch (final Throwable ex) {
                try { buffer.release(); }
                catch (final Throwable ex2) { ex.addSuppressed(ex2); }
                throw ex;
            }
            buffers.put(local.getName(), this);
        }

        E getTarget() {
            try { return output.target(); }
            catch (final IOException ex) { throw new AssertionError(ex); }
        }

        @Override
        @DischargesObligation
        public void close() throws IOException {
            final ExceptionBuilder<IOException, IOException>
                    builder = new SuppressedExceptionBuilder<>();
            if (!closed) {
                closed = true;
                try {
                    out.close();
                    final E local = output.target();
                    if (this == buffers.get(local.getName()))
                        updateProperties(local, input.target());
                    else
                        discardBuffer();
                } catch (IOException ex) {
                    builder.warn(ex);
                }
            }
            try {
                storeBuffers();
            } catch (IOException ex) {
                builder.warn(ex);
            }
            builder.check();
        }

        void updateProperties(final E local, final Entry peer) {
            for (final Access type : ALL_ACCESS)
                if (UNKNOWN == local.getTime(type))
                    local.setTime(type, peer.getTime(type));
            // Never copy any but the DATA size!
            if (UNKNOWN == local.getSize(DATA))
                local.setSize(DATA, peer.getSize(DATA));
        }

        void discardBuffer() throws IOException {
            assert closed;
            buffer.release();
        }

        boolean storeBuffer() throws IOException {
            if (!closed || isBusy()) return false;
            IoSockets.copy(input, output);
            buffer.release();
            return true;
        }
    } // BufferedEntryOutputStream
}
