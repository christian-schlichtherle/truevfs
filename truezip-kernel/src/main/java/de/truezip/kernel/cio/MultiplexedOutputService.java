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
import de.truezip.kernel.io.SequentialIOException;
import de.truezip.kernel.io.SequentialIOExceptionBuilder;
import de.truezip.kernel.util.JointIterator;
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
 * @param  <E> The type of the archive entries.
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public class MultiplexedOutputService<E extends MutableEntry>
extends DecoratingOutputService<E, OutputService<E>> {

    private final IOPool<?> pool;

    /**
     * The map of temporary archive entries which have not yet been written
     * to the output archive.
     */
    private final Map<String, BufferedEntryOutputStream> buffers
            = new LinkedHashMap<String, BufferedEntryOutputStream>();

    /** @see #isBusy */
    private boolean busy;

    /**
     * Constructs a new multiplexed output service.
     * 
     * @param output the decorated output service.
     * @param pool the pool for buffering entry data.
     */
    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    public MultiplexedOutputService(
            final @WillCloseWhenClosed OutputService<E> output,
            final IOPool<?> pool) {
        super(output);
        if (null == (this.pool = pool))
            throw new NullPointerException();
    }

    @Override
    public int size() {
        return delegate.size() + buffers.size();
    }

    @Override
    public Iterator<E> iterator() {
        return new JointIterator<E>(
                delegate.iterator(),
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
        final E entry = delegate.getEntry(name);
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
                super(MultiplexedOutputService.super.getOutputSocket(entry));
            }

            @Override
            public E getLocalTarget() throws IOException {
                return entry;
            }

            @Override
            public OutputStream newOutputStream() throws IOException {
                return isBusy()
                        ? newBufferedEntryOutputStream(getBoundDelegate())
                        : new EntryOutputStream(getBoundDelegate());
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
        } catch (final IOException ex) {
            buffer.release();
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
        delegate.close();
    }

    private void storeBuffers() throws IOException {
        if (isBusy())
            return;

        final SequentialIOExceptionBuilder<IOException, SequentialIOException> builder
                = SequentialIOExceptionBuilder.create(IOException.class, SequentialIOException.class);
        final Iterator<BufferedEntryOutputStream> i = buffers.values().iterator();
        while (i.hasNext()) {
            final BufferedEntryOutputStream out = i.next();
            boolean remove = true;
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
        @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
        EntryOutputStream(final OutputSocket<? extends E> output)
        throws IOException {
            super(output.newOutputStream());
            busy = true;
        }

        @Override
        public void close() throws IOException {
            if (closed)
                return;
            delegate.close();
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
        @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
        BufferedEntryOutputStream(  final IOBuffer<?> buffer,
                                    final OutputSocket<? extends E> output)
        throws IOException {
            super(buffer.getOutputSocket().newOutputStream());
            this.output = output;
            this.local = output.getLocalTarget();
            final Entry remote = output.getRemoteTarget();
            class InputProxy extends DecoratingInputSocket<Entry> {
                InputProxy() {
                    super(buffer.getInputSocket());
                }

                @Override
                public Entry getLocalTarget() {
                    return null != remote ? remote : buffer;
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
            delegate.close();
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
            final InputSocket<Entry> input = this.input;
            assert null != input;
            final OutputSocket<? extends E> output = this.output;
            assert null != output;
            final IOBuffer<?> buffer = this.buffer;
            assert null != buffer;
            try {
                if (!discard)
                    IOSocket.copy(input, output);
            } finally {
                buffer.release();
            }
            return true;
        }
    } // BufferedEntryOutputStream
}
