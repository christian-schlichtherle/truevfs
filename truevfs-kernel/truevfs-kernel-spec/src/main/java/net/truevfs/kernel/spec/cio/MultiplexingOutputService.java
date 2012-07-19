/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.spec.cio;

import de.schlichtherle.truecommons.io.DecoratingOutputStream;
import de.schlichtherle.truecommons.io.InputException;
import de.schlichtherle.truecommons.services.util.JointIterator;
import edu.umd.cs.findbugs.annotations.CleanupObligation;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import edu.umd.cs.findbugs.annotations.DischargesObligation;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import javax.annotation.CheckForNull;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.NotThreadSafe;
import static net.truevfs.kernel.spec.cio.Entry.ALL_ACCESS;
import net.truevfs.kernel.spec.cio.Entry.Access;
import static net.truevfs.kernel.spec.cio.Entry.Size.DATA;
import static net.truevfs.kernel.spec.cio.Entry.UNKNOWN;
import net.truevfs.kernel.spec.util.ExceptionBuilder;
import net.truevfs.kernel.spec.util.PriorityExceptionBuilder;
import net.truevfs.kernel.spec.util.SuppressedExceptionBuilder;

/**
 * Decorates another output service to support a virtually unlimited number of
 * entries which may be written concurrently while actually at most one entry
 * is written concurrently to the decorated output service.
 * If there is more than one entry to be written concurrently, the additional
 * entries are buffered to an I/O entry allocated from an I/O pool and copied
 * to the decorated output service upon a call to their
 * {@link OutputStream#close()} method.
 * Note that this implies that the {@code close()} method may fail with
 * an {@link IOException}.
 *
 * @param  <E> the type of the mutable entries.
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public class MultiplexingOutputService<E extends MutableEntry>
extends DecoratingOutputService<E, OutputService<E>> {

    private final IoBufferPool<? extends IoBuffer<?>> pool;

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
            final IoBufferPool<? extends IoBuffer<?>> pool,
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
        return new JointIterator<>(
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
            public E target() {
                return local;
            }

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
            } catch (final InputException ex) {
                builder.warn(ex);
            } catch (final IOException ex) {
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
    @CleanupObligation
    private final class EntryOutputStream extends DecoratingOutputStream {
        boolean closed;

        @CreatesObligation
        EntryOutputStream(final OutputStream out) {
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
        final IoBuffer<?> buffer;
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
            final IoBuffer<?> buffer = this.buffer = pool.allocate();
            final Entry peer = null != _peer ? _peer : buffer;
            final class InputProxy extends DecoratingInputSocket<Entry> {
                InputProxy() { super(buffer.input()); }

                @Override
                public Entry target() {
                    return peer;
                }
            } // InputProxy
            try {
                this.input = new InputProxy();
                this.out = buffer.output().stream(null);
            } catch (final Throwable ex) {
                try {
                    buffer.release();
                } catch (final Throwable ex2) {
                    ex.addSuppressed(ex2);
                }
                throw ex;
            }
            buffers.put(local.getName(), this);
        }

        E getTarget() {
            try {
                return output.target();
            } catch (final IOException ex) {
                throw new AssertionError(ex);
            }
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
                } catch (final IOException ex) {
                    builder.warn(ex);
                }
            }
            try {
                storeBuffers();
            } catch (final IOException ex) {
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

        boolean storeBuffer() throws InputException, IOException {
            if (!closed || isBusy()) return false;
            IoSockets.copy(input, output);
            buffer.release();
            return true;
        }
    } // BufferedEntryOutputStream
}
