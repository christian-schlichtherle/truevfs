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
import de.schlichtherle.truezip.entry.Entry.Size;
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
 * Decorates an {@code OutputShop} in order to support a virtually
 * unlimited number of entries which may be written concurrently while
 * actually at most one entry is written concurrently to the output archive
 * output.
 * If there is more than one entry to be written concurrently, the additional
 * entries are actually written to temp files and copied to the output
 * output archive upon a call to their {@link OutputStream#close()} method.
 * Note that this implies that the {@code close()} method may fail with
 * an {@link IOException}.
 *
 * @param   <AE> The type of the archive entries.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
@DefaultAnnotation(NonNull.class)
public class FsMultiplexedArchiveOutputShop<AE extends FsArchiveEntry>
extends DecoratingOutputShop<AE, OutputShop<AE>> {

    private final IOPool<?> pool;

    /**
     * The map of temporary archive entries which have not yet been written
     * to the output output archive.
     */
    private final Map<String, TempEntryOutputStream> temps
            = new LinkedHashMap<String, TempEntryOutputStream>();

    /** @see #isBusy */
    private boolean busy;

    /**
     * Constructs a new {@code FsMultiplexedArchiveOutputShop}.
     * 
     * @param output the decorated output archive.
     * @throws NullPointerException iff {@code output} is {@code null}.
     */
    public FsMultiplexedArchiveOutputShop(OutputShop<AE> output, final IOPool<?> pool) {
        super(output);
        this.pool = pool;
    }

    @Override
    public int getSize() {
        return delegate.getSize() + temps.size();
    }

    @Override
    public Iterator<AE> iterator() {
        return new JointIterator<AE>(delegate.iterator(), new TempEntriesIterator());
    }

    private class TempEntriesIterator implements Iterator<AE> {
        private final Iterator<TempEntryOutputStream> i
                = temps.values().iterator();

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
        final TempEntryOutputStream out = temps.get(name);
        return null == out ? null : out.getTarget();
    }

    @Override
    public OutputSocket<? extends AE> getOutputSocket(final AE entry) {
        if (null == entry)
            throw new NullPointerException();

        class Output extends DecoratingOutputSocket<AE> {
            Output() {
                super(FsMultiplexedArchiveOutputShop.super.getOutputSocket(entry));
            }

            @Override
            public OutputStream newOutputStream()
            throws IOException {
                if (isBusy()) {
                    final IOPool.Entry<?> temp = pool.allocate();
                    IOException cause = null;
                    try {
                        return new TempEntryOutputStream(getBoundSocket(), temp);
                    } catch (IOException ex) {
                        throw cause = ex;
                    } finally {
                        if (null != cause) {
                            try {
                                temp.release();
                            } catch (IOException ex) {
                                throw (IOException) ex.initCause(cause);
                            }
                        }
                    }
                } else {
                    return new EntryOutputStream(getBoundSocket().newOutputStream());
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

    /** This entry output stream writes directly to the output archive. */
    private class EntryOutputStream extends DecoratingOutputStream {
        private boolean closed;

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
                super.close();
            } finally {
                storeTemps();
            }
        }
    } // class EntryOutputStream

    /**
     * This entry output stream writes the archive entry to a temporary file.
     * When the stream is closed, the temporary file is then copied to the
     * output archive and finally deleted unless the output output
     * archive is still busy.
     */
    private class TempEntryOutputStream
    extends DecoratingOutputStream {
        private final IOPool.Entry<?> temp;
        private final OutputSocket<? extends AE> output;
        private final AE local;
        private final Entry peer;
        private final InputSocket<?> input;
        private boolean closed;

        @SuppressWarnings("LeakingThisInConstructor")
        TempEntryOutputStream(  @NonNull final OutputSocket<? extends AE> output,
                                @NonNull final IOPool.Entry<?> temp)
        throws IOException {
            super(temp.getOutputSocket().newOutputStream());
            this.output = output;
            this.local = output.getLocalTarget();
            this.peer = output.getPeerTarget();
            class ProxyInput extends DecoratingInputSocket<Entry> {
                private final Entry target = null != peer ? peer : temp;

                ProxyInput() {
                    super(temp.getInputSocket());
                }

                @Override
                public Entry getLocalTarget() {
                    return target;
                }
            }
            this.temp = temp;
            this.input = new ProxyInput();
            final TempEntryOutputStream old = temps.put(local.getName(), this);
            if (null != old)
                old.store(true);
        }

        public AE getTarget() {
            return local;
        }

        @Override
        public void close() throws IOException {
            if (closed)
                return;
            closed = true;
            try {
                try {
                    super.close();
                } finally {
                    final Entry src = input.getLocalTarget();
                    final AE dst = output.getLocalTarget();
                    for (Size type : SIZE_SET)
                        if (UNKNOWN == dst.getSize(type))
                            dst.setSize(type, src.getSize(type));
                    for (Access type : ACCESS_SET)
                        if (UNKNOWN == dst.getTime(type))
                            dst.setTime(type, src.getTime(type));
                }
            } finally {
                storeTemps();
            }
        }

        private boolean store(boolean discard) throws IOException {
            if (discard)
                assert closed : "broken archive controller!";
            else if (!closed || isBusy())
                return false;
            IOException cause = null;
            try {
                if (!discard)
                    IOSocket.copy(input, output);
            } finally {
                try {
                    temp.release();
                } catch (IOException ex) {
                    throw (IOException) ex.initCause(cause);
                }
            }
            return true;
        }
    } // class TempEntryOutputStream

    @Override
    public void close() throws IOException {
        assert !isBusy();
        try {
            storeTemps();
            assert temps.isEmpty();
        } finally {
            delegate.close();
        }
    }

    private void storeTemps() throws IOException {
        if (isBusy())
            return;

        final SequentialIOExceptionBuilder<IOException, SequentialIOException> builder
                = new SequentialIOExceptionBuilder<IOException, SequentialIOException>(
                    IOException.class, SequentialIOException.class);
        final Iterator<TempEntryOutputStream> i = temps.values().iterator();
        while (i.hasNext()) {
            final TempEntryOutputStream out = i.next();
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
