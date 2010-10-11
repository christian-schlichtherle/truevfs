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

package de.schlichtherle.truezip.io.archive.driver.tar;

import de.schlichtherle.truezip.io.rof.SimpleReadOnlyFile;
import de.schlichtherle.truezip.io.rof.ReadOnlyFile;
import de.schlichtherle.truezip.io.socket.InputSocket;
import de.schlichtherle.truezip.io.InputException;
import de.schlichtherle.truezip.io.socket.InputShop;
import de.schlichtherle.truezip.io.archive.driver.TransientIOException;
import de.schlichtherle.truezip.io.Streams;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.tools.tar.TarBuffer;
import org.apache.tools.tar.TarInputStream;
import org.apache.tools.tar.TarUtils;

import static de.schlichtherle.truezip.io.archive.driver.tar.TarDriver.TEMP_FILE_PREFIX;
import static de.schlichtherle.truezip.io.Files.createTempFile;
import static org.apache.tools.tar.TarConstants.GIDLEN;
import static org.apache.tools.tar.TarConstants.MODELEN;
import static org.apache.tools.tar.TarConstants.MODTIMELEN;
import static org.apache.tools.tar.TarConstants.NAMELEN;
import static org.apache.tools.tar.TarConstants.SIZELEN;
import static org.apache.tools.tar.TarConstants.UIDLEN;

/**
 * Presents a {@link TarInputStream} as a randomly accessible archive.
 * <p>
 * <b>Warning:</b> 
 * The constructor of this class extracts each entry in the archive to a
 * temporary file.
 * This may be very time and space consuming for large archives, but is
 * the fastest implementation for subsequent random access, since there
 * is no way the archive driver could predict the client application's
 * behaviour.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class TarInputShop
implements InputShop<TarEntry> {

    private static final byte[] NULL_RECORD = new byte[TarBuffer.DEFAULT_RCDSIZE];

    private static final int CHECKSUM_OFFSET
            = NAMELEN + MODELEN + UIDLEN + GIDLEN + SIZELEN + MODTIMELEN;

    /** Maps entry names to tar entries [String -> TarEntry]. */
    private final Map<String, TarEntry> entries
            = new LinkedHashMap<String, TarEntry>();

    /**
     * Extracts the entire TAR input stream into a temporary directory in order
     * to allow subsequent random access to its entries.
     *
     * @param in The input stream from which this input archive file should be
     *        initialized. This stream is not used by any of the methods in
     *        this class after the constructor has terminated and is
     *        <em>never</em> closed!
     *        So it is safe and recommended to close it upon termination
     *        of this constructor.
     */
    public TarInputShop(final InputStream in)
    throws IOException {
        final TarInputStream tin = newValidatedTarInputStream(in);
        try {
            org.apache.tools.tar.TarEntry tinEntry;
            while ((tinEntry = tin.getNextEntry()) != null) {
                final String name = tinEntry.getName();
                TarEntry entry;
                if (tinEntry.isDirectory()) {
                    entry = new TarEntry(tinEntry);
                } else {
                    final File tmp;
                    try {
                        entry = entries.get(name);
                        tmp = entry != null
                                ? entry.getFile()
                                : createTempFile(TEMP_FILE_PREFIX);
                        try {
                            final java.io.FileOutputStream out
                                    = new java.io.FileOutputStream(tmp);
                            try {
                                Streams.cat(tin, out); // use high performance pump (async I/O)
                            } finally {
                                out.close();
                            }
                        } catch (IOException ex) {
                            final boolean ok = tmp.delete();
                            assert ok;
                            throw ex;
                        }
                    } catch (InputException ex) {
                        throw ex;
                    } catch (IOException ex) {
                        throw new TransientIOException(
                                new TempFileException(tinEntry, ex));
                    }
                    entry = new TarEntry(tinEntry, tmp);
                }
                entry.setName(name); // use normalized name
                entries.put(name, entry);
            }
        } catch (IOException failure) {
            close0();
            throw failure;
        }
    }

    /**
     * Returns a newly created and validated {@link TarInputStream}.
     * This method performs a simple validation by computing the checksum
     * for the first record only.
     * This method is required because the {@code TarInputStream}
     * unfortunately does not do any validation!
     */
    private static TarInputStream newValidatedTarInputStream(
            final InputStream in)
    throws IOException {
        final byte[] buf = new byte[TarBuffer.DEFAULT_RCDSIZE];
        final InputStream vin = readAhead(in, buf);
        // If the record is the null record, the TAR file is empty and we're
        // done with validating.
        if (!Arrays.equals(buf, NULL_RECORD)) {
            final long expected = TarUtils.parseOctal(buf, CHECKSUM_OFFSET, 8);
            for (int i = 0; i < 8; i++)
                buf[CHECKSUM_OFFSET + i] = ' ';
            final long is = TarUtils.computeCheckSum(buf);
            if (expected != is)
                throw new IOException(
                        "Illegal initial record in TAR file: Expected checksum " + expected + ", is " + is + "!");
        }
        return new TarInputStream(
                vin, TarBuffer.DEFAULT_BLKSIZE, TarBuffer.DEFAULT_RCDSIZE);
    }

    /**
     * Fills {@code buf} with data from the given input stream and
     * returns an input stream from which you can still read all data,
     * including the data in buf.
     *
     * @param in The stream to read from. May <em>not</em> be {@code null}.
     * @param buf The buffer to fill entirely with data.
     * @return A stream which holds all the data {@code in} did.
     * @throws IOException If {@code buf} couldn't get filled entirely.
     */
    static InputStream readAhead(final InputStream in, final byte[] buf)
    throws IOException {
        if (in.markSupported()) {
            in.mark(buf.length);
            readFully(in, buf);
            in.reset();
            return in;
        } else {
            final PushbackInputStream pin
                    = new PushbackInputStream(in, buf.length);
            readFully(pin, buf);
            pin.unread(buf);
            return pin;
        }
    }
    
    private static void readFully(final InputStream in, final byte[] buf)
    throws IOException {
        final int l = buf.length;
        int n = 0;
        do  {
            final int r = in.read(buf, n, l - n);
            if (r == -1)
                throw new EOFException();
            n += r;
        } while (n < l);
    }

    @Override
    public int size() {
        return entries.size();
    }

    @Override
    public Iterator<TarEntry> iterator() {
        return entries.values().iterator();
    }

    @Override
    public TarEntry getEntry(String name) {
        return entries.get(name);
    }

    @Override
    public InputSocket<TarEntry> newInputSocket(final TarEntry entry)
    throws FileNotFoundException {
        if (getEntry(entry.getName()) != entry)
            throw new IllegalArgumentException("interface contract violation");
        class Input extends InputSocket<TarEntry> {
            @Override
            public TarEntry getLocalTarget() {
                return entry;
            }

            @Override
            public InputStream newInputStream()
            throws IOException {
                return new FileInputStream(entry.getFile());
            }

            @Override
            public ReadOnlyFile newReadOnlyFile() throws IOException {
                return new SimpleReadOnlyFile(entry.getFile());
            }
        }
        return new Input();
    }

    @Override
    public void close() throws IOException {
        close0();
    }

    private void close0() throws IOException {
        final Collection<TarEntry> values = entries.values();
        for (final Iterator<TarEntry> i = values.iterator(); i.hasNext(); i.remove()) {
            final TarEntry entry = i.next();
            final File file = entry.getFile();
            if (file == null) {
                assert entry.isDirectory();
                continue;
            }
            assert file.exists();
            if (!file.delete()) {
                // Windoze: The temp file is still open for reading by one
                // or more entry input streams.
                file.deleteOnExit();
            }
        }
    }

    /**
     * This needs to be a {@link FileNotFoundException} in order to signal that
     * the TAR is simply not accessible and not necessarily a false positive.
     */
    private static final class TempFileException extends FileNotFoundException {
        private static final long serialVersionUID = 1923814625681036853L;

        private TempFileException(
                final org.apache.tools.tar.TarEntry entry,
                final IOException cause) {
            super(entry.getName() + " (couldn't create temp file for archive entry)");
            initCause(cause);
        }
    }
}
