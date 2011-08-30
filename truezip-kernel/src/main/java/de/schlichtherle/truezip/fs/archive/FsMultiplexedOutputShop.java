/*
 * Copyright (C) 2006-2011 Schlichtherle IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.schlichtherle.truezip.fs.archive;

import de.schlichtherle.truezip.entry.Entry;
import static de.schlichtherle.truezip.entry.Entry.*;
import de.schlichtherle.truezip.entry.Entry.Access;
import static de.schlichtherle.truezip.entry.Entry.Size.*;
import de.schlichtherle.truezip.io.DecoratingOutputStream;
import de.schlichtherle.truezip.io.InputException;
import de.schlichtherle.truezip.io.SequentialIOException;
import de.schlichtherle.truezip.io.SequentialIOExceptionBuilder;
import de.schlichtherle.truezip.socket.DecoratingInputSocket;
import de.schlichtherle.truezip.socket.DecoratingOutputShop;
import de.schlichtherle.truezip.socket.DecoratingOutputSocket;
import de.schlichtherle.truezip.socket.IOPool;
import de.schlichtherle.truezip.socket.IOSocket;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.socket.OutputShop;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.util.JointIterator;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import net.jcip.annotations.NotThreadSafe;

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
 * @param   <AE> The type of the archive entries.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
@DefaultAnnotation(NonNull.class)
public class FsMultiplexedOutputShop<AE extends FsArchiveEntry>
extends DecoratingOutputShop<AE, OutputShop<AE>> {

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
    public FsMultiplexedOutputShop(
            final OutputShop<AE> output,
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
    public Iterator<AE> iterator() {
        return new JointIterator<AE>(
                delegate.iterator(),
                new BufferedEntriesIterator());
    }

    private class BufferedEntriesIterator implements Iterator<AE> {
        final Iterator<BufferedEntryOutputStream> i = buffers.values().iterator();

        @Override
        public boolean hasNext() {
            return i.hasNext();
        }

        @Override
        public AE next() {
            return i.next().getTarget();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public @CheckForNull AE getEntry(String name) {
        final AE entry = delegate.getEntry(name);
        if (null != entry)
            return entry;
        final BufferedEntryOutputStream out = buffers.get(name);
        return null == out ? null : out.getTarget();
    }

    @Override
    public OutputSocket<? extends AE> getOutputSocket(final AE entry) {
        if (null == entry)
            throw new NullPointerException();

        class Output extends DecoratingOutputSocket<AE> {
            Output() {
                super(FsMultiplexedOutputShop.super.getOutputSocket(entry));
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
                        try {
                            temp.release();
                        } catch (IOException discard) {
                        }
                        throw ex;
                    }
                } else {
                    return new EntryOutputStream(
                            getBoundSocket().newOutputStream());
                }
            }
        } // class Output

        return new Output();
    }

    /**
     * Returns whether the container output archive is busy writing an archive
     * entry or not.
     */
    public boolean isBusy() {
        return busy;
    }

    /** This entry output stream writes directly to this output shop. */
    private class EntryOutputStream extends DecoratingOutputStream {
        boolean closed;

        EntryOutputStream(final OutputStream out) {
            super(out);
            busy = true;
        }

        @Override
        public void close() throws IOException {
            if (closed)
                return;
            closed = true;
            busy = false;
            try {
                delegate.close();
            } finally {
                storeBuffers();
            }
        }
    } // class EntryOutputStream

    /**
     * This entry output stream writes the archive entry to an
     * {@link de.schlichtherle.truezip.socket.IOPool.Entry I/O pool entry}.
     * When the stream gets closed, the I/O pool entry is then copied to this
     * output shop and finally deleted unless this output shop is still busy.
     */
    private class BufferedEntryOutputStream extends DecoratingOutputStream {
        final IOPool.Entry<?> temp;
        final OutputSocket<? extends AE> output;
        final AE local;
        final InputSocket<Entry> input;
        boolean closed;

        @SuppressWarnings("LeakingThisInConstructor")
        BufferedEntryOutputStream(
                final IOPool.Entry<?> temp,
                final OutputSocket<? extends AE> output)
        throws IOException {
            super(temp.getOutputSocket().newOutputStream());
            this.output = output;
            this.local = output.getLocalTarget();
            final Entry peer = output.getPeerTarget();
            class ProxyInput extends DecoratingInputSocket<Entry> {
                ProxyInput() {
                    super(temp.getInputSocket());
                }

                @Override
                public Entry getLocalTarget() {
                    return null != peer ? peer : temp;
                }
            }
            this.temp = temp;
            this.input = new ProxyInput();
            final BufferedEntryOutputStream
                    old = buffers.put(local.getName(), this);
            if (null != old)
                old.store(true);
        }

        AE getTarget() {
            return local;
        }

        @Override
        public void close() throws IOException {
            if (closed)
                return;
            closed = true;
            try {
                delegate.close();
            } finally {
                try {
                    final Entry src = input.getLocalTarget();
                    final AE dst = getTarget();
                    // Never copy anything but the DATA size!
                    if (UNKNOWN == dst.getSize(DATA))
                        dst.setSize(DATA, src.getSize(DATA));
                    for (Access type : ALL_ACCESS_SET)
                        if (UNKNOWN == dst.getTime(type))
                            dst.setTime(type, src.getTime(type));
                } finally {
                    storeBuffers();
                }
            }
        }

        boolean store(boolean discard) throws IOException {
            if (discard)
                assert closed : "broken archive controller!";
            else if (!closed || isBusy())
                return false;
            try {
                if (!discard)
                    IOSocket.copy(input, output);
            } finally {
                temp.release();
            }
            return true;
        }
    } // class BufferedEntryOutputStream

    @Override
    public void close() throws IOException {
        if (isBusy())
            throw new IOException("Output shop is still busy!");
        try {
            storeBuffers();
            assert buffers.isEmpty();
        } finally {
            delegate.close();
        }
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
}
