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
import edu.umd.cs.findbugs.annotations.CreatesObligation;
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
    @CreatesObligation
    public MultiplexingOutputService(
            final @WillCloseWhenClosed OutputService<E> output,
            final IOPool<?> pool) {
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
    public @CheckForNull E getEntry(String name) {
        final E entry = container.getEntry(name);
        if (null != entry)
            return entry;
        final BufferedEntryOutputStream out = buffers.get(name);
        return null == out ? null : out.getTarget();
    }

    @Override
    public OutputSocket<E> getOutputSocket(final E entry) {
        if (null == entry)
            throw new NullPointerException();

        class Output extends DecoratingOutputSocket<E> {
            Output() {
                super(MultiplexingOutputService.super.getOutputSocket(entry));
            }

            @Override
            public E getLocalTarget() throws IOException {
                return entry;
            }

            @Override
            public OutputStream stream() throws IOException {
                return isBusy()
                        ? newBufferedEntryOutputStream(getBoundSocket())
                        : new EntryOutputStream(getBoundSocket());
            }
        } // Output

        return new Output();
    }

    private BufferedEntryOutputStream newBufferedEntryOutputStream(
            final OutputSocket<? extends E> output)
    throws IOException {
        final IOBuffer<?> buffer = pool.allocate();
        try {
            return new BufferedEntryOutputStream(buffer, output);
        } catch (final Throwable ex) {
            try {
                buffer.release();
            } catch (final IOException ex2) {
                ex.addSuppressed(ex2);
            }
            throw ex;
        }
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
        final Iterator<BufferedEntryOutputStream> i = buffers.values().iterator();
        while (i.hasNext()) {
            final BufferedEntryOutputStream out = i.next();
            boolean remove = false;
            try {
                remove = out.store(false);
            } catch (final InputException ex) {
                builder.warn(ex);
            } catch (final IOException ex) {
                throw builder.fail(ex);
            } finally {
                if (remove)
                    i.remove();
            }
        }
        builder.check();
    }

    /** This entry output stream writes directly to this output service. */
    private class EntryOutputStream extends DecoratingOutputStream {
        boolean closed;

        @CreatesObligation
        EntryOutputStream(final OutputSocket<? extends E> output)
        throws IOException {
            super(output.stream());
            busy = true;
        }

        @Override
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
    private class BufferedEntryOutputStream extends DecoratingOutputStream {
        final InputSocket<Entry> input;
        final OutputSocket<? extends E> output;
        final IOBuffer<?> buffer;
        final E local;
        boolean closed;

        @CreatesObligation
        @SuppressWarnings("LeakingThisInConstructor")
        BufferedEntryOutputStream(  final IOBuffer<?> buffer,
                                    final OutputSocket<? extends E> output)
        throws IOException {
            super(buffer.getOutputSocket().stream());
            this.output = output;
            this.local = output.getLocalTarget();
            final Entry peer = output.getPeerTarget();
            class InputProxy extends DecoratingInputSocket<Entry> {
                InputProxy() {
                    super(buffer.getInputSocket());
                }

                @Override
                public Entry getLocalTarget() {
                    return null != peer ? peer : buffer;
                }
            }
            this.buffer = buffer;
            this.input = new InputProxy();
            final BufferedEntryOutputStream
                    old = buffers.put(local.getName(), this);
            if (null != old)
                old.store(true);
        }

        E getTarget() {
            return local;
        }

        @Override
        public void close() throws IOException {
            if (closed)
                return;
            out.close();
            closed = true;
            copyProperties();
            storeBuffers();
        }

        void copyProperties() throws IOException {
            final Entry src = input.getLocalTarget();
            final E dst = getTarget();
            // Never copy anything but the DATA size!
            if (UNKNOWN == dst.getSize(DATA))
                dst.setSize(DATA, src.getSize(DATA));
            for (final Access type : ALL_ACCESS_SET)
                if (UNKNOWN == dst.getTime(type))
                    dst.setTime(type, src.getTime(type));
        }

        boolean store(final boolean discard) throws IOException {
            if (discard)
                assert closed : "broken archive controller!";
            else if (!closed || isBusy())
                return false;
            Throwable ex = null;
            try {
                if (!discard)
                    IOSocket.copy(input, output);
            } catch (final Throwable ex2) {
                ex = ex2;
                throw ex2;
            } finally {
                try {
                    buffer.release();
                } catch (final IOException ex2) {
                    if (null == ex)
                        throw ex2;
                    ex.addSuppressed(ex2);
                }
            }
            return true;
        }
    } // BufferedEntryOutputStream
}
