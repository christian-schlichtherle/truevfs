/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.archive;

import de.schlichtherle.truezip.entry.Entry;
import static de.schlichtherle.truezip.entry.Entry.ALL_ACCESS_SET;
import de.schlichtherle.truezip.entry.Entry.Access;
import static de.schlichtherle.truezip.entry.Entry.Size.DATA;
import static de.schlichtherle.truezip.entry.Entry.UNKNOWN;
import de.schlichtherle.truezip.io.DecoratingOutputStream;
import de.schlichtherle.truezip.io.InputException;
import de.schlichtherle.truezip.io.SequentialIOException;
import de.schlichtherle.truezip.io.SequentialIOExceptionBuilder;
import de.schlichtherle.truezip.socket.*;
import de.schlichtherle.truezip.util.JointIterator;
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
 * Decorates annother output shop to support a virtually unlimited number of
 * entries which may be written concurrently while actually at most one entry
 * is written concurrently to the decorated output shop.
 * If there is more than one entry to be written concurrently, the additional
 * entries are buffered to an I/O entry allocated from an I/O pool and copied
 * to the decorated output shop upon a call to their
 * {@link OutputStream#close()} method.
 * Note that this implies that the {@code close()} method may fail with
 * an {@link IOException}.
 *
 * @param   <E> The type of the archive entries.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
public class FsMultiplexedOutputShop<E extends FsArchiveEntry>
extends DecoratingOutputShop<E, OutputShop<E>> {

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
     * Constructs a new multiplexed output shop.
     * 
     * @param output the decorated output shop.
     * @param pool the pool for buffering entry data.
     */
    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    public FsMultiplexedOutputShop(
            final @WillCloseWhenClosed OutputShop<E> output,
            final IOPool<?> pool) {
        super(output);
        if (null == pool)
            throw new NullPointerException();
        this.pool = pool;
    }

    @Override
    public int getSize() {
        return delegate.getSize() + buffers.size();
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
    public OutputSocket<? extends E> getOutputSocket(final E entry) {
        class Output extends DecoratingOutputSocket<E> {
            Output() {
                super(FsMultiplexedOutputShop.super.getOutputSocket(entry));
            }

            @Override
            public E getLocalTarget() throws IOException {
                return entry;
            }

            @Override
            public OutputStream newOutputStream()
            throws IOException {
                if (isBusy()) {
                    final IOPool.Entry<?> temp = pool.allocate();
                    try {
                        return new BufferedEntryOutputStream(
                                temp, getBoundSocket());
                    } catch (IOException ex) {
                        temp.release();
                        throw ex;
                    }
                } else {
                    return new EntryOutputStream(getBoundSocket());
                }
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
    public void close() throws IOException {
        if (isBusy())
            throw new IOException("Output shop is still busy!");
        storeBuffers();
        assert buffers.isEmpty();
        delegate.close();
    }

    private void storeBuffers() throws IOException {
        if (isBusy())
            return;

        final SequentialIOExceptionBuilder<IOException, SequentialIOException> builder
                = new SequentialIOExceptionBuilder<IOException, SequentialIOException>(
                    IOException.class, SequentialIOException.class);
        final Iterator<BufferedEntryOutputStream> i = buffers.values().iterator();
        while (i.hasNext()) {
            final BufferedEntryOutputStream out = i.next();
            boolean remove = true;
            try {
                remove = out.store(false);
            } catch (InputException ex) {
                builder.warn(ex); // let's continue anyway...
            } catch (IOException ex) {
                throw builder.fail(ex); // something's wrong writing this MultiplexedOutputStream!
            } finally {
                if (remove)
                    i.remove();
            }
        }
        builder.check();
    }

    /** This entry output stream writes directly to this output shop. */
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
     * {@link de.schlichtherle.truezip.socket.IOPool.Entry I/O pool entry}.
     * When the stream gets closed, the I/O pool entry is then copied to this
     * output shop and finally deleted unless this output shop is still busy.
     */
    private class BufferedEntryOutputStream extends DecoratingOutputStream {
        final InputSocket<Entry> input;
        final OutputSocket<? extends E> output;
        final IOPool.Entry<?> buffer;
        final E local;
        boolean closed;

        @CreatesObligation
        @SuppressWarnings("LeakingThisInConstructor")
        @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
        BufferedEntryOutputStream(
                final IOPool.Entry<?> buffer,
                final OutputSocket<? extends E> output)
        throws IOException {
            super(buffer.getOutputSocket().newOutputStream());
            this.output = output;
            this.local = output.getLocalTarget();
            final Entry peer = output.getPeerTarget();
            class ProxyInputSocket extends DecoratingInputSocket<Entry> {
                ProxyInputSocket() {
                    super(buffer.getInputSocket());
                }

                @Override
                public Entry getLocalTarget() {
                    return null != peer ? peer : buffer;
                }
            }
            this.buffer = buffer;
            this.input = new ProxyInputSocket();
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

        boolean store(boolean discard) throws IOException {
            if (discard)
                assert closed : "broken archive controller!";
            else if (!closed || isBusy())
                return false;
            final InputSocket<Entry> input = this.input;
            assert null != input;
            final OutputSocket<? extends E> output = this.output;
            assert null != output;
            final IOPool.Entry<?> buffer = this.buffer;
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
