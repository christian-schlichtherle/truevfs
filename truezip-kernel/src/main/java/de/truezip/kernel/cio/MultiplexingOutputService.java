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
import edu.umd.cs.findbugs.annotations.CleanupObligation;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
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
            return i.next().getLocalTarget();
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
        return null == out ? null : out.getLocalTarget();
    }

    @Override
    public OutputSocket<E> output(final E entry) {
        if (null == entry)
            throw new NullPointerException();

        final class Output extends DecoratingOutputSocket<E> {
            Output() {
                super(container.output(entry));
            }

            @Override
            public E localTarget() {
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

        IOException ex = null;
        for (   final Iterator<BufferedEntryOutputStream> i = buffers.values().iterator();
                i.hasNext(); ) {
            final BufferedEntryOutputStream out = i.next();
            try {
                if (out.storeBuffer())
                    i.remove();
            } catch (final InputException ex2) {
                if (null != ex)
                    ex.addSuppressed(ex2);
                else
                    ex = ex2;
            } catch (final IOException ex2) {
                if (null != ex)
                    ex2.addSuppressed(ex);
                throw ex2;
            }
        }
        if (null != ex)
            throw ex;
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
            busy = false;
            closed = true;
            storeBuffers();
        }
    } // EntryOutputStream

    /**
     * This entry output stream writes the archive entry to an
     * {@linkplain IOBuffer I/O buffer}.
     * When the stream gets closed, the I/O buffer is then copied to this
     * output service and finally deleted unless this output service is still busy.
     */
    @CleanupObligation
    private final class BufferedEntryOutputStream
    extends DecoratingOutputStream {
        final InputSocket<?> input;
        final OutputSocket<? extends E> output;
        final IOBuffer<?> buffer;
        boolean closed;

        @CreatesObligation
        @SuppressWarnings("LeakingThisInConstructor")
        BufferedEntryOutputStream(final OutputSocket<? extends E> output)
        throws IOException {
            // HC SUNT DRACONES!
            final E local = (this.output = output).localTarget();
            final Entry _peer = output.peerTarget();
            final IOBuffer<?> buffer = this.buffer = pool.allocate();
            final Entry peer = null != _peer ? _peer : buffer;
            final class InputProxy extends DecoratingInputSocket<Entry> {
                InputProxy() { super(buffer.input()); }

                @Override
                public Entry localTarget() {
                    return peer;
                }
            } // InputProxy
            try {
                this.input = new InputProxy();
                this.out = buffer.output().stream();
            } catch (final Throwable ex) {
                try {
                    buffer.release();
                } catch (final IOException ex2) {
                    ex.addSuppressed(ex2);
                }
                throw ex;
            }
            buffers.put(local.getName(), this);
        }

        E getLocalTarget() {
            try {
                return output.localTarget();
            } catch (final IOException ex) {
                throw new AssertionError(ex);
            }
        }

        @Override
        @DischargesObligation
        public void close() throws IOException {
            if (closed)
                return;
            out.close();
            closed = true;
            saveBuffers();
        }

        void saveBuffers() throws IOException {
            Throwable ex = null;
            try {
                final E local = output.localTarget();
                final Entry peer = input.localTarget();
                if (this == buffers.get(local.getName()))
                    updateProperties(local, peer);
                else
                    discardBuffer();
            } catch (final Throwable ex2) {
                ex = ex2;
                throw ex2;
            } finally {
                try {
                    storeBuffers();
                } catch (final IOException ex2) {
                    if (null == ex)
                        throw ex2;
                    ex.addSuppressed(ex2);
                }
            }
        }

        void updateProperties(final E local, final Entry peer) {
            // Never copy any but the DATA size!
            if (UNKNOWN == local.getSize(DATA))
                local.setSize(DATA, peer.getSize(DATA));
            for (final Access type : ALL_ACCESS_SET)
                if (UNKNOWN == local.getTime(type))
                    local.setTime(type, peer.getTime(type));
        }

        void discardBuffer() throws IOException {
            assert closed;
            buffer.release();
        }

        boolean storeBuffer() throws InputException, IOException {
            if (!closed || isBusy())
                return false;
            IOSocket.copy(input, output);
            buffer.release();
            return true;
        }
    } // BufferedEntryOutputStream
}
