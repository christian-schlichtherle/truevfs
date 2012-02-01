/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.archive.tar;

import static de.schlichtherle.truezip.entry.EntryName.SEPARATOR;
import static de.schlichtherle.truezip.entry.EntryName.SEPARATOR_CHAR;
import de.schlichtherle.truezip.fs.FsEntryName;
import de.schlichtherle.truezip.io.Streams;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.socket.IOPool;
import de.schlichtherle.truezip.socket.IOPool.Entry;
import de.schlichtherle.truezip.socket.InputShop;
import de.schlichtherle.truezip.socket.InputSocket;
import static de.schlichtherle.truezip.util.Maps.initialCapacity;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.io.*;
import java.util.*;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import static org.apache.commons.compress.archivers.tar.TarConstants.*;
import org.apache.commons.compress.archivers.tar.TarUtils;

/**
 * Presents a {@link TarArchiveInputStream} as a randomly accessible archive.
 * <p>
 * <b>Warning:</b> 
 * The constructor of this class extracts each entry in the archive to a
 * temporary file.
 * This may be very time and space consuming for large archives, but is
 * the fastest implementation for subsequent random access, since there
 * is no way the archive driver could predict the client application's
 * behaviour.
 *
 * @see     TarOutputShop
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public class TarInputShop
implements InputShop<TTarArchiveEntry> {

    /** Default record size */
    private static final int DEFAULT_RCDSIZE = 512;

    /** Default block size */
    public static final int DEFAULT_BLKSIZE = 20 * DEFAULT_RCDSIZE * 20;

    private static final byte[] NULL_RECORD = new byte[DEFAULT_RCDSIZE];

    private static final int CHECKSUM_OFFSET
            = NAMELEN + MODELEN + UIDLEN + GIDLEN + SIZELEN + MODTIMELEN;

    /** Maps entry names to I/O pool entries. */
    private final Map<String, TTarArchiveEntry>
            entries = new LinkedHashMap<String, TTarArchiveEntry>(
                    initialCapacity(TarOutputShop.OVERHEAD_SIZE));

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
    public TarInputShop(final TarDriver driver, final InputStream in)
    throws IOException {
        final TarArchiveInputStream tin = newValidatedTarInputStream(in);
        final IOPool<?> pool = driver.getPool();
        try {
            TarArchiveEntry tinEntry;
            while (null != (tinEntry = (TarArchiveEntry) tin.getNextEntry())) {
                final String name = getName(tinEntry);
                TTarArchiveEntry entry = entries.get(name);
                if (null != entry)
                    entry.release();
                entry = new TTarArchiveEntry(name, tinEntry);
                if (!tinEntry.isDirectory()) {
                    final Entry<?> temp = pool.allocate();
                    entry.setTemp(temp);
                    try {
                        final OutputStream
                                out = temp.getOutputSocket().newOutputStream();
                        try {
                            Streams.cat(tin, out);
                        } finally {
                            out.close();
                        }
                    } catch (IOException ex) {
                        temp.release();
                        throw ex;
                    }
                }
                entries.put(name, entry);
            }
        } catch (IOException ex) {
            close0();
            throw ex;
        }
    }

    /**
     * Returns the fixed name of the given TAR entry, ensuring that it ends
     * with a {@link FsEntryName#SEPARATOR} if it's a directory.
     *
     * @param entry the TAR entry.
     * @return the fixed name of the given TAR entry.
     * @see <a href="http://java.net/jira/browse/TRUEZIP-62">Issue TRUEZIP-62</a>
     */
    private static String getName(ArchiveEntry entry) {
        final String name = entry.getName();
        return entry.isDirectory() && !name.endsWith(SEPARATOR) ? name + SEPARATOR_CHAR : name;
    }

    /**
     * Returns a newly created and validated {@link TarArchiveInputStream}.
     * This method performs a simple validation by computing the checksum
     * for the first record only.
     * This method is required because the {@code TarArchiveInputStream}
     * unfortunately does not do any validation!
     */
    private static TarArchiveInputStream newValidatedTarInputStream(
            final InputStream in)
    throws IOException {
        final byte[] buf = new byte[DEFAULT_RCDSIZE];
        final InputStream vin = readAhead(in, buf);
        // If the record is the null record, the TAR file is empty and we're
        // done with validating.
        if (!Arrays.equals(buf, NULL_RECORD)) {
            try {
                final long expected = TarUtils.parseOctal(buf, CHECKSUM_OFFSET, 8);
                for (int i = 0; i < 8; i++)
                    buf[CHECKSUM_OFFSET + i] = ' ';
                final long is = TarUtils.computeCheckSum(buf);
                if (expected != is)
                    throw new IOException(
                            "Illegal initial record in TAR file: Expected checksum " + expected + ", is " + is + "!");
            } catch (IllegalArgumentException ex) {
                throw new IOException("Illegal initial record in TAR file!");
            }
        }
        return new TarArchiveInputStream(vin, DEFAULT_BLKSIZE, DEFAULT_RCDSIZE);
    }

    /**
     * Fills {@code buf} with data from the given input stream and
     * returns an input stream from which you can still read all data,
     * including the data in buf.
     *
     * @param  in The stream to read from. May <em>not</em> be {@code null}.
     * @param  buf The buffer to fill entirely with data.
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
            if (0 >= r)
                throw new EOFException();
            n += r;
        } while (n < l);
    }

    @Override
    public final int getSize() {
        return entries.size();
    }

    @Override
    public final Iterator<TTarArchiveEntry> iterator() {
        return entries.values().iterator();
    }

    @Override
    public final @CheckForNull TTarArchiveEntry getEntry(String name) {
        return entries.get(name);
    }

    @Override
    public InputSocket<TTarArchiveEntry> getInputSocket(final String name) {
        if (null == name)
            throw new NullPointerException();

        class Input extends InputSocket<TTarArchiveEntry> {
            @Override
            public TTarArchiveEntry getLocalTarget() throws IOException {
                final TTarArchiveEntry entry = getEntry(name);
                if (null == entry)
                    throw new FileNotFoundException(name + " (entry not found)");
                if (entry.isDirectory())
                    throw new FileNotFoundException(name + " (cannot read directories)");
                return entry;
            }

            @Override
            public ReadOnlyFile newReadOnlyFile() throws IOException {
                return getLocalTarget().getTemp().getInputSocket().newReadOnlyFile();
            }

            @Override
            public InputStream newInputStream()
            throws IOException {
                return getLocalTarget().getTemp().getInputSocket().newInputStream();
            }
        } // class Input

        return new Input();
    }

    @Override
    public void close() throws IOException {
        close0();
    }

    private void close0() throws IOException {
        Collection<TTarArchiveEntry> values = entries.values();
        for (Iterator<TTarArchiveEntry> i = values.iterator(); i.hasNext(); i.remove())
            i.next().release();
    }
}
