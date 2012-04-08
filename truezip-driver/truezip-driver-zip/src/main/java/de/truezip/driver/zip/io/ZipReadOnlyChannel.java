/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.io;

import de.truezip.kernel.io.LockInputStream;
import de.truezip.kernel.rof.DefaultReadOnlyFile;
import de.truezip.kernel.rof.ReadOnlyFile;
import de.truezip.kernel.util.Pool;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.ZipException;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Drop-in replacement for {@link java.util.zip.ZipFile java.util.zip.ZipFile}.
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
public class ZipReadOnlyChannel extends RawReadOnlyChannel<ZipEntry> {

    /** The lock on which this object synchronizes. */
    protected final Lock lock = new ReentrantLock();

    private final String name;
    
    private volatile @CheckForNull ZipCryptoParameters cryptoParameters;

    /**
     * Equivalent to {@link #ZipReadOnlyChannel(String, Charset, boolean, boolean)
     * ZipReadOnlyChannel(name, DEFAULT_CHARSET, true, false)}
     */
    public ZipReadOnlyChannel(String path)
    throws IOException {
        this(path, DEFAULT_CHARSET, true, false);
    }

    /**
     * Equivalent to {@link #ZipReadOnlyChannel(String, Charset, boolean, boolean)
     * ZipReadOnlyChannel(name, charset, true, false)}
     */
    public ZipReadOnlyChannel(String path, Charset charset)
    throws IOException {
        this(path, charset, true, false);
    }

    /**
     * Opens the ZIP file identified by the given path name for reading its
     * entries.
     *
     * @param path the path name of the file.
     * @param charset the charset to use for decoding entry names and ZIP file
     *        comment.
     * @param preambled if this is {@code true}, then the ZIP file may have a
     *        preamble.
     *        Otherwise, the ZIP file must start with either a Local File
     *        Header (LFH) signature or an End Of Central Directory (EOCD)
     *        Header, causing this constructor to fail if the file is actually
     *        a false positive ZIP file, i.e. not compatible to the ZIP File
     *        Format Specification.
     *        This may be useful to read Self Extracting ZIP files (SFX), which
     *        usually contain the application code required for extraction in
     *        the preamble.
     * @param postambled if this is {@code true}, then the ZIP file may have a
     *        postamble of arbitrary length.
     *        Otherwise, the ZIP file must not have a postamble which exceeds
     *        64KB size, including the End Of Central Directory record
     *        (i.e. including the ZIP file comment), causing this constructor
     *        to fail if the file is actually a false positive ZIP file, i.e.
     *        not compatible to the ZIP File Format Specification.
     *        This may be useful to read Self Extracting ZIP files (SFX) with
     *        large postambles.
     * @throws FileNotFoundException if {@code name} cannot get opened for
     *         reading.
     * @throws ZipException if {@code name} is not compatible with the ZIP
     *         File Format Specification.
     * @throws IOException on any other I/O related issue.
     * @see    #recoverLostEntries()
     */
    public ZipReadOnlyChannel(
            final String path,
            final Charset charset,
            final boolean preambled,
            final boolean postambled)
    throws IOException {
        super(  new DefaultReadOnlyChannelPool(path),
                new DefaultZipFileParameters(charset, preambled, postambled));
        this.name = path;
    }

    /**
     * Equivalent to {@link #ZipReadOnlyChannel(File, Charset, boolean, boolean)
     * ZipReadOnlyChannel(file, DEFAULT_CHARSET, true, false)}
     */
    public ZipReadOnlyChannel(File file)
    throws IOException {
        this(file, DEFAULT_CHARSET, true, false);
    }

    /**
     * Equivalent to {@link #ZipReadOnlyChannel(File, Charset, boolean, boolean)
     * ZipReadOnlyChannel(file, charset, true, false)}
     */
    public ZipReadOnlyChannel(File file, Charset charset)
    throws IOException {
        this(file, charset, true, false);
    }

    /**
     * Opens the given {@link File} for reading its entries.
     *
     * @param file the file.
     * @param charset the charset to use for decoding entry names and ZIP file
     *        comment.
     * @param preambled if this is {@code true}, then the ZIP file may have a
     *        preamble.
     *        Otherwise, the ZIP file must start with either a Local File
     *        Header (LFH) signature or an End Of Central Directory (EOCD)
     *        Header, causing this constructor to fail if the file is actually
     *        a false positive ZIP file, i.e. not compatible to the ZIP File
     *        Format Specification.
     *        This may be useful to read Self Extracting ZIP files (SFX), which
     *        usually contain the application code required for extraction in
     *        the preamble.
     * @param postambled if this is {@code true}, then the ZIP file may have a
     *        postamble of arbitrary length.
     *        Otherwise, the ZIP file must not have a postamble which exceeds
     *        64KB size, including the End Of Central Directory record
     *        (i.e. including the ZIP file comment), causing this constructor
     *        to fail if the file is actually a false positive ZIP file, i.e.
     *        not compatible to the ZIP File Format Specification.
     *        This may be useful to read Self Extracting ZIP files (SFX) with
     *        large postambles.
     * @throws FileNotFoundException if {@code file} cannot get opened for
     *         reading.
     * @throws ZipException if {@code file} is not compatible with the ZIP
     *         File Format Specification.
     * @throws IOException on any other I/O related issue.
     * @see    #recoverLostEntries()
     */
    public ZipReadOnlyChannel(
            final File file,
            final Charset charset,
            final boolean preambled,
            final boolean postambled)
    throws IOException {
        super(  new DefaultReadOnlyChannelPool(file),
                new DefaultZipFileParameters(charset, preambled, postambled));
        this.name = file.toString();
    }

    /**
     * Equivalent to {@link #ZipReadOnlyChannel(ReadOnlyFile, Charset, boolean, boolean)
     * ZipReadOnlyChannel(rof, DEFAULT_CHARSET, true, false)}
     */
    public ZipReadOnlyChannel(SeekableByteChannel channel)
    throws IOException {
        this(channel, DEFAULT_CHARSET, true, false);
    }

    /**
     * Equivalent to {@link #ZipReadOnlyChannel(ReadOnlyFile, Charset, boolean, boolean)
     * ZipReadOnlyChannel(rof, charset, true, false)}
     */
    public ZipReadOnlyChannel(SeekableByteChannel channel, Charset charset)
    throws IOException {
        this(channel, charset, true, false);
    }

    /**
     * Opens the given {@link ReadOnlyFile} for reading its entries.
     *
     * @param channel the channel to read.
     * @param charset the charset to use for decoding entry names and ZIP file
     *        comment.
     * @param preambled if this is {@code true}, then the ZIP file may have a
     *        preamble.
     *        Otherwise, the ZIP file must start with either a Local File
     *        Header (LFH) signature or an End Of Central Directory (EOCD)
     *        Header, causing this constructor to fail if the file is actually
     *        a false positive ZIP file, i.e. not compatible to the ZIP File
     *        Format Specification.
     *        This may be useful to read Self Extracting ZIP files (SFX), which
     *        usually contain the application code required for extraction in
     *        the preamble.
     * @param postambled if this is {@code true}, then the ZIP file may have a
     *        postamble of arbitrary length.
     *        Otherwise, the ZIP file must not have a postamble which exceeds
     *        64KB size, including the End Of Central Directory record
     *        (i.e. including the ZIP file comment), causing this constructor
     *        to fail if the file is actually a false positive ZIP file, i.e.
     *        not compatible to the ZIP File Format Specification.
     *        This may be useful to read Self Extracting ZIP files (SFX) with
     *        large postambles.
     * @throws FileNotFoundException if {@code rof} cannot get opened for
     *         reading.
     * @throws ZipException if {@code rof} is not compatible with the ZIP
     *         File Format Specification.
     * @throws IOException on any other I/O related issue.
     * @see    #recoverLostEntries()
     */
    public ZipReadOnlyChannel(
            SeekableByteChannel channel,
            Charset charset,
            boolean preambled,
            boolean postambled)
    throws IOException {
        super(channel, new DefaultZipFileParameters(charset, preambled, postambled));
        this.name = channel.toString();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Note that this method is <em>not</em> thread-safe!
     */
    @Override
    public void recoverLostEntries() throws IOException {
        super.recoverLostEntries();
    }

    /**
     * Returns the {@link Object#toString() string representation} of whatever
     * input source object was used to construct this ZIP file.
     * For {@link String} and {@link File} objects, this is a path name.
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
            final Iterator<ZipEntry> i = ZipReadOnlyChannel.super.iterator();

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
            final Iterator<ZipEntry> i = ZipReadOnlyChannel.super.iterator();

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
     * Returns a clone of the entry for the given name or {@code null} if no
     * entry with this name exists.
     *
     * @param name the name of the ZIP entry.
     */
    @Override
    public ZipEntry getEntry(String name) {
        final ZipEntry ze = super.getEntry(name);
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
        return new LockInputStream(in, lock);
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
        return new LockInputStream(in, lock);
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
        return in == null ? null : new LockInputStream(in, lock);
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
     * A pool which allocates {@link DefaultReadOnlyFile} objects for the
     * file provided to its constructor.
     */
    private static final class DefaultReadOnlyChannelPool
    implements Pool<SeekableByteChannel, IOException> {
        final File file;

        DefaultReadOnlyChannelPool(String name) {
            this(new File(name));
        }

        DefaultReadOnlyChannelPool(final File file) {
            this.file = file;
        }

        @Override
        public SeekableByteChannel allocate() throws IOException {
            return FileChannel.open(file.toPath());
        }

        @Override
        public void release(SeekableByteChannel channel) throws IOException {
            channel.close();
        }
    } // DefaultReadOnlyChannelPool
}