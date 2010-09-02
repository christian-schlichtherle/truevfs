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

package de.schlichtherle.truezip.io.archive.driver;

import de.schlichtherle.truezip.io.archive.entry.ArchiveEntry;
import de.schlichtherle.truezip.io.util.ChainableIOException;
import de.schlichtherle.truezip.io.util.ChainableIOExceptionBuilder;
import de.schlichtherle.truezip.io.util.InputException;
import de.schlichtherle.truezip.io.socket.IORef;
import de.schlichtherle.truezip.io.socket.Sockets;
import de.schlichtherle.truezip.util.JointIterator;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import static de.schlichtherle.truezip.io.archive.entry.ArchiveEntry.UNKNOWN;
import static de.schlichtherle.truezip.io.util.Files.createTempFile;

/**
 * A decorator for output archives which allows to write an unlimited number
 * of entries concurrently while at most one entry is actually concurrently
 * written to the target output archive.
 * If there is more than one entry to be written concurrently, the additional
 * entries are actually written to temp files and copied to the target
 * output archive upon a call to their {@link OutputStream#close()} method.
 * Note that this implies that the {@code close()} method may fail with
 * an {@link IOException}.
 * <p>
 * Implementations do <em>not</em> need to be thread-safe:
 * Multithreading needs to be addressed by client applications.
 *
 * @param   <AE> The run time type of the archive entries in this container.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public class MultiplexedOutputArchive<AE extends ArchiveEntry>
extends FilterOutputArchive<AE> {

    /** Prefix for temporary files created by the multiplexer. */
    static final String TEMP_FILE_PREFIX = "tzp-mux";

    /**
     * The map of temporary archive entries which have not yet been written
     * to the target output archive.
     */
    private final Map<String, TempEntryOutputStream> temps
            = new LinkedHashMap<String, TempEntryOutputStream>();

    /** @see #isTargetBusy */
    private boolean targetBusy;

    /**
     * Constructs a new {@code MultiplexedOutputArchive}.
     * 
     * @param target the decorated output archive.
     * @throws NullPointerException iff {@code target} is {@code null}.
     */
    public MultiplexedOutputArchive(final OutputArchive<AE> target) {
        super(target);
        if (target == null)
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
            throw new UnsupportedOperationException("entry removal");
        }
    }

    @Override
    public AE getEntry(String name) {
        AE entry = target.getEntry(name);
        if (entry != null)
            return entry;
        final TempEntryOutputStream out = temps.get(name);
        return out != null ? out.getTarget() : null;
    }

    @Override
    public ArchiveOutputStreamSocket<AE> getOutputStreamSocket(final AE entry)
    throws FileNotFoundException {
        final ArchiveOutputStreamSocket<AE> dst
                = super.getOutputStreamSocket(entry);
        class OutputProxy implements ArchiveOutputStreamSocket<AE> {
            @Override
            public AE getTarget() {
                return entry;
            }

            @Override
            public OutputStream newOutputStream(
                    final IORef<? extends ArchiveEntry> src)
            throws IOException {
                return MultiplexedOutputArchive.this.newOutputStream(dst, src);
            }
        } // class OutputProxy
        return new OutputProxy();
    }

    protected OutputStream newOutputStream(
            final ArchiveOutputStreamSocket<AE> dst,
            final IORef<? extends ArchiveEntry> src)
    throws IOException {
        final ArchiveEntry entry = dst.getTarget();
        if (src != null)
            entry.setSize(src.getTarget().getSize()); // data may be compressed!
        return isTargetBusy()
                ? new TempEntryOutputStream(
                    createTempFile(TEMP_FILE_PREFIX), dst, src)
                : new EntryOutputStream(dst.newOutputStream(src));
    }

    /**
     * Returns whether the target output archive is busy writing an archive
     * entry or not.
     */
    public boolean isTargetBusy() {
        return targetBusy;
    }

    /**
     * This entry output stream writes directly to the target output archive.
     */
    private class EntryOutputStream extends FilterOutputStream {
        private boolean closed;

        EntryOutputStream(final OutputStream out)
        throws IOException {
            super(out);
            targetBusy = true;
        }

        @Override
        public void write(byte[] b) throws IOException {
            out.write(b, 0, b.length);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            out.write(b, off, len);
        }

        @Override
        public void close() throws IOException {
            if (closed)
                return;

            // Order is important here!
            closed = true;
            targetBusy = false;
            super.close();
            storeTemps();
        }
    } // class EntryOutputStream

    /**
     * This entry output stream writes the archive entry to a temporary file.
     * When the stream is closed, the temporary file is then copied to the
     * target output archive and finally deleted unless the target output
     * archive is still busy.
     */
    private class TempEntryOutputStream
    extends FileOutputStream
    implements IORef<AE> {
        private final File temp;
        private final ArchiveOutputStreamSocket<AE> dst;
        private final ArchiveInputStreamSocket<ArchiveEntry> src;
        private boolean closed;

        @SuppressWarnings("LeakingThisInConstructor")
        TempEntryOutputStream(
                final File temp,
                final ArchiveOutputStreamSocket<AE> dst,
                final IORef<? extends ArchiveEntry> src)
        throws IOException {
            super(temp);
            this.temp = temp;
            this.dst = dst;
            this.src = src instanceof ArchiveInputStreamSocket
                    ? (ArchiveInputStreamSocket) src
                    : new TempInputStreamSocket();
            temps.put(dst.getTarget().getName(), this);
        }

        class TempInputStreamSocket
        implements ArchiveInputStreamSocket<ArchiveEntry> {
            private final ArchiveEntry entry = new RfsEntry(temp);

            @Override
            public ArchiveEntry getTarget() {
                return entry;
            }

            @Override
            public InputStream newInputStream(IORef<? extends ArchiveEntry> destination)
            throws IOException {
                return new FileInputStream(temp);
            }
        } // class TempInputStreamSocket

        @Override
        public AE getTarget() {
            return dst.getTarget();
        }

        @Override
        public void close() throws IOException {
            if (closed)
                return;

            // Order is important here!
            // Note that this must be guarded by the closed flag:
            // close() gets called from the finalize() method in the
            // subclass, which may cause a ConcurrentModificationException
            // in this method.
            closed = true;
            try {
                super.close();
            } finally {
                final AE entry = dst.getTarget();
                final ArchiveEntry srcEntry = src.getTarget();
                if (entry.getSize() == UNKNOWN)
                    entry.setSize(srcEntry.getSize());
                if (entry.getTime() == UNKNOWN)
                    entry.setTime(srcEntry.getTime());
                storeTemps();
            }
        }

        boolean store() throws IOException {
            if (!closed || isTargetBusy())
                return false;

            try {
                Sockets.copy(src, dst);
            } finally {
                if (!temp.delete()) // may fail on Windoze if in.close() failed!
                    temp.deleteOnExit(); // be bullish never to leavy any temps!
            }
            return true;
        }
    } // class TempEntryOutputStream

    @Override
    public void close() throws IOException {
        assert !isTargetBusy();
        try {
            storeTemps();
            assert temps.isEmpty();
        } finally {
            target.close();
        }
    }

    private void storeTemps() throws IOException {
        if (isTargetBusy())
            return;

        final ChainableIOExceptionBuilder<ChainableIOException> builder
                = new ChainableIOExceptionBuilder<ChainableIOException>();
        final Iterator<TempEntryOutputStream> i = temps.values().iterator();
        while (i.hasNext()) {
            final TempEntryOutputStream out = i.next();
            boolean remove = true;
            try {
                remove = out.store();
            } catch (InputException ex) {
                // Input exception - let's continue!
                builder.warn(new ChainableIOException(ex));
            } catch (IOException ex) {
                // Something's wrong writing this MultiplexedOutputStream!
                throw builder.fail(new ChainableIOException(ex));
            } finally {
                if (remove)
                    i.remove();
            }
        }
        builder.check();
    }
}
