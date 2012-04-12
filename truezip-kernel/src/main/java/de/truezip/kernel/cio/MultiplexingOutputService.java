/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.cio;

import static de.truezip.kernel.cio.Entry.ALL_ACCESS_SET;
import de.truezip.kernel.cio.Entry.Access;
import static de.truezip.kernel.cio.Entry.Size.DATA;
import static de.truezip.kernel.cio.Entry.UNKNOWN;
import de.truezip.kernel.io.DecoratingOutputStream;
import de.truezip.kernel.io.InputException;
import de.truezip.kernel.util.JointIterator;
import de.truezip.kernel.util.SuppressedExceptionBuilder;
import edu.umd.cs.findbugs.annotations.DischargesObligation;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.NotThreadSafe;

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

    private final IOPool<?> pool;

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
            final IOPool<?> pool,
            final @WillCloseWhenClosed OutputService<E> output) {
        super(output);
        if (null == (this.pool = pool))
            throw new NullPointerException();
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
        if (null != entry)
            return entry;
        final BufferedEntryOutputStream out = buffers.get(name);
        return null == out ? null : out.getTarget();
    }

    @Override
    public OutputSocket<E> outputSocket(final E entry) {
        if (null == entry)
            throw new NullPointerException();

        final class Output extends DecoratingOutputSocket<E> {
            Output() {
                super(MultiplexingOutputService.super.outputSocket(entry));
            }

            @Override
            public E localTarget() throws IOException {
                return entry;
            }

            @Override
            public OutputStream stream() throws IOException {
                final OutputSocket<? extends E> output = getBoundSocket();
                return isBusy() ? new BufferedEntryOutputStream(output)
                                : new EntryOutputStream(output);
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
            throw new IOException("Output service is still busy!");
        storeBuffers();
        assert buffers.isEmpty();
        container.close();
    }

    private void storeBuffers() throws IOException {
        if (isBusy())
            return;

        final SuppressedExceptionBuilder<IOException>
                builder = new SuppressedExceptionBuilder<>();
        for (   final Iterator<BufferedEntryOutputStream> i = buffers.values().iterator();
                i.hasNext(); ) {
            final BufferedEntryOutputStream out = i.next();
            try {
                if (out.store())
                    i.remove();
            } catch (final InputException ex) {
                builder.warn(ex);
            } catch (final IOException ex) {
                throw builder.fail(ex);
            }
        }
        builder.check();
    }

    /** This entry output stream writes directly to this output service. */
    private final class EntryOutputStream extends DecoratingOutputStream {
        boolean closed;

        EntryOutputStream(final OutputSocket<? extends E> output)
        throws IOException {
            super(output.stream());
            busy = true;
        }

        @Override
        @DischargesObligation
        public void close() throws IOException {
            if (closed)
                return;
            out.close();
            closed = true;
            busy = false;
            storeBuffers();
        }
    } // EntryOutputStream

    /**
     * This entry output stream writes the archive entry to an
     * {@linkplain IOBuffer I/O buffer}.
     * When the stream gets closed, the I/O buffer is then copied to this
     * output service and finally deleted unless this output service is still busy.
     */
    private final class BufferedEntryOutputStream
    extends DecoratingOutputStream {
        final E local;
        final InputSocket<Entry> input;
        final OutputSocket<? extends E> output;
        final IOBuffer<?> buffer;
        final BufferedEntryOutputStream next;
        boolean closed;

        @SuppressWarnings("LeakingThisInConstructor")
        BufferedEntryOutputStream(final OutputSocket<? extends E> output)
        throws IOException {
            // HC SUNT DRACONES!
            final E local = this.local = (this.output = output).localTarget();
            final Entry peer = output.peerTarget();
            final IOBuffer<?> buffer = this.buffer = pool.allocate();
            final class InputProxy extends DecoratingInputSocket<Entry> {
                InputProxy() {
                    super(buffer.inputSocket());
                }

                @Override
                public Entry localTarget() {
                    return null != peer ? peer : buffer;
                }
            } // InputProxy
            try {
                this.input = new InputProxy();
                this.out = buffer.outputSocket().stream();
            } catch (final Throwable ex) {
                try {
                    buffer.release();
                } catch (final IOException ex2) {
                    ex.addSuppressed(ex2);
                }
                throw ex;
            }
            this.next = buffers.put(local.getName(), this);
            assert null == next;
        }

        E getTarget() {
            return local;
        }

        @Override
        @DischargesObligation
        public void close() throws IOException {
            if (closed)
                return;
            out.close();
            closed = true;
            copyProperties();
            storeBuffers();
        }

        void copyProperties() throws IOException {
            final Entry src = input.localTarget();
            final E dst = local;
            // Never copy anything but the DATA size!
            if (UNKNOWN == dst.getSize(DATA))
                dst.setSize(DATA, src.getSize(DATA));
            for (final Access type : ALL_ACCESS_SET)
                if (UNKNOWN == dst.getTime(type))
                    dst.setTime(type, src.getTime(type));
        }

        boolean store() throws InputException, IOException {
            if (!closed || isBusy())
                return false;
            IOSocket.copy(input, output);
            buffer.release();
            return true;
        }
    } // BufferedEntryOutputStream
}
