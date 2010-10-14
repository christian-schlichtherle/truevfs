/*
 * Copyright (C) 2006-2010 Schlichtherle IT Services
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
package de.schlichtherle.truezip.io.archive.output;

import de.schlichtherle.truezip.io.FilterOutputStream;
import de.schlichtherle.truezip.io.entry.CommonEntry.Size;
import de.schlichtherle.truezip.io.socket.FilterOutputSocket;
import de.schlichtherle.truezip.io.rof.SimpleReadOnlyFile;
import de.schlichtherle.truezip.io.rof.ReadOnlyFile;
import de.schlichtherle.truezip.io.socket.InputSocket;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.io.entry.CommonEntry.Access;
import de.schlichtherle.truezip.io.socket.OutputShop;
import de.schlichtherle.truezip.io.socket.FilterOutputShop;
import de.schlichtherle.truezip.io.socket.OutputSocket;
import de.schlichtherle.truezip.io.entry.CommonEntry;
import de.schlichtherle.truezip.io.entry.FileEntry;
import de.schlichtherle.truezip.io.socket.IOSocket;
import de.schlichtherle.truezip.io.ChainableIOException;
import de.schlichtherle.truezip.io.ChainableIOExceptionBuilder;
import de.schlichtherle.truezip.io.InputException;
import de.schlichtherle.truezip.io.archive.entry.ArchiveEntry;
import de.schlichtherle.truezip.io.entry.TempFilePool;
import de.schlichtherle.truezip.util.JointIterator;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import static de.schlichtherle.truezip.io.archive.entry.ArchiveEntry.UNKNOWN;

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
 * <p>
 * Implementations do <em>not</em> need to be thread-safe:
 * Multithreading needs to be addressed by client applications.
 *
 * @param   <AE> The type of the archive entries.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public class MultiplexedArchiveOutputShop<AE extends ArchiveEntry>
extends FilterOutputShop<AE, OutputShop<AE>> {

    /**
     * The map of temporary archive entries which have not yet been written
     * to the output output archive.
     */
    private final Map<String, TempEntryOutputStream> temps
            = new LinkedHashMap<String, TempEntryOutputStream>();

    /** @see #isBusy */
    private boolean busy;

    /**
     * Constructs a new {@code MultiplexedArchiveOutputShop}.
     * 
     * @param output the decorated output archive.
     * @throws NullPointerException iff {@code output} is {@code null}.
     */
    public MultiplexedArchiveOutputShop(final OutputShop<AE> output) {
        super(output);
        if (output == null)
            throw new NullPointerException();
    }

    @Override
    public int size() {
        return target.size() + temps.size();
    }

    @Override
    public Iterator<AE> iterator() {
        return new JointIterator<AE>(target.iterator(), new TempEntriesIterator());
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
    public AE getEntry(String name) {
        final AE entry = target.getEntry(name);
        if (null != entry)
            return entry;
        final TempEntryOutputStream out = temps.get(name);
        return null == out ? null : out.getTarget();
    }

    @Override
    public OutputSocket<AE> getOutputSocket(final AE entry)
    throws IOException {
        class Output extends FilterOutputSocket<AE> {
            Output() throws IOException {
                super(MultiplexedArchiveOutputShop.super.getOutputSocket(entry));
            }

            @Override
            public OutputStream newOutputStream()
            throws IOException {
                if (isBusy()) {
                    final OutputSocket<? extends AE> socket = getOutputSocket();
                    final FileEntry temp = TempFilePool.get().allocate();
                    IOException cause = null;
                    try {
                        return new TempEntryOutputStream(socket, temp);
                    } catch (IOException ex) {
                        throw cause = ex;
                    } finally {
                        if (null != cause) {
                            try {
                                TempFilePool.get().release(temp);
                            } catch (IOException ex) {
                                throw (IOException) ex.initCause(cause);
                            }
                        }
                    }
                } else {
                    return new EntryOutputStream(super.newOutputStream());
                }
            }
        }
        return new Output();
    }

    /**
     * Returns whether the target output archive is busy writing an archive
     * entry or not.
     */
    public boolean isBusy() {
        return busy;
    }

    /** This entry output stream writes directly to the output archive. */
    private class EntryOutputStream extends FilterOutputStream {
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
    extends FilterOutputStream {
        private final FileEntry temp;
        private final OutputSocket<? extends AE> output;
        private final AE local;
        private final CommonEntry remote;
        private final InputSocket<CommonEntry> input;
        private boolean closed;

        @SuppressWarnings({"LeakingThisInConstructor", "ThrowableInitCause"})
        TempEntryOutputStream(  final OutputSocket<? extends AE> output,
                                final FileEntry temp)
        throws IOException {
            super(new FileOutputStream(temp.getTarget())); // Do NOT extend FileIn|OutputStream: They implement finalize(), which may cause deadlocks!
            this.output = output;
            this.local = output.getLocalTarget();
            this.remote = output.getRemoteTarget();
            class Input extends InputSocket<CommonEntry> {
                private final CommonEntry target = null == remote ? temp : remote;

                @Override
                public CommonEntry getLocalTarget() {
                    return target;
                }

                @Override
                public InputStream newInputStream() throws IOException {
                    return new FileInputStream(temp.getTarget());
                }

                @Override
                public ReadOnlyFile newReadOnlyFile() throws IOException {
                    return new SimpleReadOnlyFile(temp.getTarget());
                }
            }
            this.temp = temp;
            this.input = new Input();
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
                super.close();
            } finally {
                try {
                    final CommonEntry src = input.getLocalTarget();
                    final AE dst = output.getLocalTarget();
                    for (final Size type : BitField.allOf(Size.class))
                        if (UNKNOWN == dst.getSize(type))
                            dst.setSize(type, src.getSize(type));
                    for (final Access type : BitField.allOf(Access.class))
                        if (UNKNOWN == dst.getTime(type))
                            dst.setTime(type, src.getTime(type));
                } finally {
                    storeTemps();
                }
            }
        }

        @SuppressWarnings("ThrowableInitCause")
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
                    TempFilePool.get().release(temp);
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
            super.close();
        }
    }

    private void storeTemps() throws IOException {
        if (isBusy())
            return;

        final ChainableIOExceptionBuilder<IOException, ChainableIOException> builder
                = new ChainableIOExceptionBuilder<IOException, ChainableIOException>(
                    IOException.class, ChainableIOException.class);
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
