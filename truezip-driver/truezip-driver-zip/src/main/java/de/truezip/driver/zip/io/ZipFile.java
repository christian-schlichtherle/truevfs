/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.io;

import de.truezip.kernel.io.AbstractSource;
import de.truezip.kernel.io.LockInputStream;
import de.truezip.kernel.io.OneTimeSource;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import static java.nio.file.Files.newByteChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.ZipException;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Replacement for {@link java.util.zip.ZipFile java.util.zip.ZipFile}.
 * <p>
 * Where the constructors of this class accept a {@code charset}
 * parameter, this is used to decode comments and entry names in the ZIP file.
 * However, if an entry has bit 11 set in its General Purpose Bit Flag,
 * then this parameter is ignored and "UTF-8" is used for this entry.
 * This is in accordance to Appendix D of PKWARE's ZIP File Format
 * Specification, version 6.3.0 and later.
 * <p>
 * This class is able to skip a preamble like the one found in self extracting
 * archives.
 * <p>
 * Note that the entries returned by this class are instances of
 * {@code de.truezip.kernel.io.zip.ZipEntry} instead of
 * {@code java.util.zip.ZipEntry}.
 *
 * @see    ZipOutputStream
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class ZipFile extends RawFile<ZipEntry> {

    /** The lock on which this object synchronizes. */
    protected final Lock lock = new ReentrantLock();

    private final String name;
    
    private volatile @CheckForNull ZipCryptoParameters cryptoParameters;

    /**
     * Equivalent to {@link #ZipFile(Path, Charset, boolean, boolean)
     * ZipFile(file, DEFAULT_CHARSET, true, false)}
     */
    public ZipFile(Path file)
    throws IOException {
        this(file, DEFAULT_CHARSET, true, false);
    }

    /**
     * Equivalent to {@link #ZipFile(Path, Charset, boolean, boolean)
     * ZipFile(file, charset, true, false)}
     */
    public ZipFile(Path file, Charset charset)
    throws IOException {
        this(file, charset, true, false);
    }

    /**
     * Opens the given {@code file} for reading its entries.
     *
     * @param  file the file.
     * @param  charset the charset to use for decoding entry names and ZIP file
     *         comment.
     * @param  preambled if this is {@code true}, then the ZIP file may have a
     *         preamble.
     *         Otherwise, the ZIP file must start with either a Local File
     *         Header (LFH) signature or an End Of Central Directory (EOCD)
     *         Header, causing this constructor to fail if the file is actually
     *         a false positive ZIP file, i.e. not compatible to the ZIP File
     *         Format Specification.
     *         This may be useful to read Self Extracting ZIP files (SFX),
     *         which usually contain the application code required for
     *         extraction in the preamble.
     * @param  postambled if this is {@code true}, then the ZIP file may have a
     *         postamble of arbitrary length.
     *         Otherwise, the ZIP file must not have a postamble which exceeds
     *         64KB size, including the End Of Central Directory record
     *         (i.e. including the ZIP file comment), causing this constructor
     *         to fail if the file is actually a false positive ZIP file, i.e.
     *         not compatible to the ZIP File Format Specification.
     *         This may be useful to read Self Extracting ZIP files (SFX) with
     *         large postambles.
     * @throws ZipException if the file data is not compatible with the ZIP
     *         File Format Specification.
     * @throws EOFException on premature end-of-file.
     * @throws IOException on any I/O error.
     * @see    #recoverLostEntries()
     */
    public ZipFile(
            final Path file,
            final Charset charset,
            final boolean preambled,
            final boolean postambled)
    throws ZipException, EOFException, IOException {
        super(  new ZipSource(file),
                new DefaultZipFileParameters(charset, preambled, postambled));
        this.name = file.toString();
    }

    /**
     * Equivalent to {@link #ZipFile(SeekableByteChannel, Charset, boolean, boolean)
     * ZipFile(rof, DEFAULT_CHARSET, true, false)}
     */
    public ZipFile(SeekableByteChannel channel)
    throws IOException {
        this(channel, DEFAULT_CHARSET, true, false);
    }

    /**
     * Equivalent to {@link #ZipFile(SeekableByteChannel, Charset, boolean, boolean)
     * ZipFile(rof, charset, true, false)}
     */
    public ZipFile(SeekableByteChannel channel, Charset charset)
    throws IOException {
        this(channel, charset, true, false);
    }

    /**
     * Opens the given {@link SeekableByteChannel} for reading its entries.
     *
     * @param  channel the channel to read.
     * @param  charset the charset to use for decoding entry names and ZIP file
     *         comment.
     * @param  preambled if this is {@code true}, then the ZIP file may have a
     *         preamble.
     *         Otherwise, the ZIP file must start with either a Local File
     *         Header (LFH) signature or an End Of Central Directory (EOCD)
     *         Header, causing this constructor to fail if the file is actually
     *         a false positive ZIP file, i.e. not compatible to the ZIP File
     *         Format Specification.
     *         This may be useful to read Self Extracting ZIP files (SFX),
     *         which usually contain the application code required for
     *         extraction in the preamble.
     * @param  postambled if this is {@code true}, then the ZIP file may have a
     *         postamble of arbitrary length.
     *         Otherwise, the ZIP file must not have a postamble which exceeds
     *         64KB size, including the End Of Central Directory record
     *         (i.e. including the ZIP file comment), causing this constructor
     *         to fail if the file is actually a false positive ZIP file, i.e.
     *         not compatible to the ZIP File Format Specification.
     *         This may be useful to read Self Extracting ZIP files (SFX) with
     *         large postambles.
     * @throws ZipException if the channel data is not compatible with the ZIP
     *         File Format Specification.
     * @throws EOFException on premature end-of-file.
     * @throws IOException on any I/O error.
     * @see    #recoverLostEntries()
     */
    public ZipFile(
            SeekableByteChannel channel,
            Charset charset,
            boolean preambled,
            boolean postambled)
    throws ZipException, EOFException, IOException {
        super(  new OneTimeSource(channel),
                new DefaultZipFileParameters(charset, preambled, postambled));
        this.name = channel.toString();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Note that this method is <em>not</em> thread-safe!
     */
    @Override
    public ZipFile recoverLostEntries() throws IOException {
        super.recoverLostEntries();
        return this;
    }

    /**
     * Returns the {@link Object#toString() string representation} of whatever
     * input source object was used to construct this ZIP file.
     * For {@link String} and {@link Path} objects, this is a path name.
     */
    public String getName() {
        return name;
    }

    /**
     * Enumerates clones of all entries in this ZIP file.
     *
     * @see #iterator()
     */
    public Enumeration<? extends ZipEntry> entries() {
        final class CloneEnumeration implements Enumeration<ZipEntry> {
            final Iterator<ZipEntry> i = ZipFile.super.iterator();

            @Override
            public boolean hasMoreElements() {
                return i.hasNext();
            }

            @Override
            public ZipEntry nextElement() {
                return i.next().clone();
            }
        } // CloneEnumeration

        return new CloneEnumeration();
    }

    /**
     * Iterates through clones for all entries in this ZIP file.
     * The iteration does not support element removal.
     */
    @Override
    public Iterator<ZipEntry> iterator() {
        final class EntryIterator implements Iterator<ZipEntry> {
            final Iterator<ZipEntry> i = ZipFile.super.iterator();

            @Override
            public boolean hasNext() {
                return i.hasNext();
            }

            @Override
            public ZipEntry next() {
                return i.next().clone();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        } // EntryIterator

        return new EntryIterator();
    }

    /**
     * Returns a clone of the entry for the given {@code name} or {@code null}
     * if no entry with this name exists in this ZIP file.
     *
     * @param  name the name of the ZIP entry.
     * @return A clone of the entry for the given {@code name} or {@code null}
     *         if no entry with this name exists in this ZIP file.
     */
    @Override
    public ZipEntry entry(String name) {
        final ZipEntry ze = super.entry(name);
        return ze != null ? ze.clone() : null;
    }

    @Override
    @SuppressWarnings("deprecation")
    public InputStream getPreambleInputStream() throws IOException {
        final InputStream in;
        lock.lock();
        try {
            in = super.getPreambleInputStream();
        } finally {
            lock.unlock();
        }
        return new LockInputStream(lock, in);
    }

    @Override
    @SuppressWarnings("deprecation")
    public InputStream getPostambleInputStream() throws IOException {
        final InputStream in;
        lock.lock();
        try {
            in = super.getPostambleInputStream();
        } finally {
            lock.unlock();
        }
        return new LockInputStream(lock, in);
    }

    @Override
    public boolean busy() {
        lock.lock();
        try {
            return super.busy();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public @Nullable ZipCryptoParameters getCryptoParameters() {
        return cryptoParameters;
    }

    /**
     * Sets the parameters for encryption or authentication of entries.
     * 
     * @param cryptoParameters the parameters for encryption or authentication
     *        of entries.
     */
    public void setCryptoParameters(
            final @CheckForNull ZipCryptoParameters cryptoParameters) {
        this.cryptoParameters = cryptoParameters;
    }

    @Override
    @SuppressWarnings("deprecation")
    protected InputStream getInputStream(
            String name, Boolean check, boolean process)
    throws  IOException {
        final InputStream in;
        lock.lock();
        try {
            in = super.getInputStream(name, check, process);
        } finally {
            lock.unlock();
        }
        return in == null ? null : new LockInputStream(lock, in);
    }

    @Override
    public void close() throws IOException {
        lock.lock();
        try {
            super.close();
        } finally {
            lock.unlock();
        }
    }

    /**
     * A pool which allocates {@link SeekableByteChannel} objects for the
     * file provided to its constructor.
     */
    private static final class ZipSource extends AbstractSource {
        final Path file;

        ZipSource(final Path file) {
            this.file = file;
        }

        @Override
        public SeekableByteChannel channel() throws IOException {
            return newByteChannel(file);
        }
    } // ZipSource
}
