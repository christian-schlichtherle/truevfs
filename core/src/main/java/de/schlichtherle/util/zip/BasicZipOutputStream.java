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

package de.schlichtherle.util.zip;

import de.schlichtherle.io.util.*;

import java.io.*;
import java.util.*;
import java.util.zip.*;

/**
 * <em>This class is <b>not</b> intended for public use!</em>
 * The methods in this class are unsynchronized and
 * {@link #entries}/{@link #getEntry} enumerate/return {@link ZipEntry}
 * instances which are shared with this class rather than clones of them.
 * This may be used by subclasses in order to benefit from the slightly better
 * performance.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 * @since TrueZIP 6.4
 * @see ZipOutputStream
 */
public class BasicZipOutputStream extends FilterOutputStream {

    /**
     * The default character set used for entry names and comments in ZIP
     * compatible files.
     * This is {@value} for compatibility with Sun's JDK implementation.
     * Note that you should use &quot;IBM437&quot; for ordinary ZIP files
     * instead.
     */
    public static final String DEFAULT_CHARSET = ZIP.DEFAULT_CHARSET;

    /** The charset to use for entry names and comments. */
    private final String charset;

    /** CRC instance to avoid parsing DEFLATED data twice. */
    private final CRC32 crc = new CRC32();

    /** This instance is used for deflated output. */
    private final ZipDeflater def = new ZipDeflater();

    /** This buffer holds deflated data for output. */
    private final byte[] dbuf = new byte[ZIP.FLATER_BUF_LENGTH];

    private final byte[] sbuf = new byte[1];

    /** The file comment. */
    private String comment = "";

    /** Default compression method for next entry. */
    private short method = ZIP.DEFLATED;

    /**
     * The list of ZIP entries started to be written so far.
     * Maps entry names to zip entries.
     */
    private final Map entries = new LinkedHashMap();

    /** Start of entry data. */
    private long dataStart;

    /** Start of central directory. */
    private long cdOffset;

    private boolean finished;

    private boolean closed;

    /** Current entry. */
    private ZipEntry entry;

    /**
     * Whether or not we need to deflate the current entry.
     * This can be used together with the <code>DEFLATED</code> method to
     * write already compressed entry data into the ZIP file.
     */
    private boolean deflate;

    /**
     * Creates a new ZIP output stream decorating the given output stream,
     * using the {@value #DEFAULT_CHARSET} charset.
     *
     * @throws NullPointerException If <code>out</code> is <code>null</code>.
     */
    public BasicZipOutputStream(final OutputStream out)
    throws NullPointerException {
        super(toLEDataOutputStream(out));

        // Check parameters (fail fast).
        if (out == null)
            throw new NullPointerException();

        this.charset = DEFAULT_CHARSET;
    }

    /**
     * Creates a new ZIP output stream decorating the given output stream.
     *
     * @throws NullPointerException If <code>out</code> or <code>charset</code> is
     *         <code>null</code>.
     * @throws UnsupportedEncodingException If charset is not supported by
     *         this JVM.
     */
    public BasicZipOutputStream(
            final OutputStream out,
            final String charset)
    throws  NullPointerException,
            UnsupportedEncodingException {
        super(toLEDataOutputStream(out));

        // Check parameters (fail fast).
        if (out == null || charset == null)
            throw new NullPointerException();
        "".getBytes(charset); // may throw UnsupportedEncodingException!

        this.charset = charset;
    }

    private static LEDataOutputStream toLEDataOutputStream(OutputStream out) {
        return out instanceof LEDataOutputStream
                ? (LEDataOutputStream) out
                : new LEDataOutputStream(out);
    }

    /**
     * Returns the charset to use for filenames and the file comment.
     */
    public String getEncoding() {
        return charset;
    }

    /**
     * Returns the number of ZIP entries written so far.
     */
    public int size() {
        return entries.size();
    }

    /**
     * Returns an enumeration of the ZIP entries written so far.
     * Note that the enumerated entries are shared with this class.
     * It is illegal to put more entries into this ZIP output stream
     * concurrently or modify the state of the enumerated entries.
     */
    public Enumeration entries() {
        return Collections.enumeration(entries.values());
    }

    /**
     * Returns the {@link ZipEntry} for the given name or
     * <code>null</code> if no entry with that name exists.
     * Note that the returned entry is shared with this class.
     * It is illegal to change its state!
     *
     * @param name Name of the ZIP entry.
     */
    public ZipEntry getEntry(String name) {
        return (ZipEntry) entries.get(name);
    }

    /**
     * Returns the file comment.
     */
    public String getComment() {
        return comment;
    }

    /**
     * Sets the file comment.
     */
    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * Returns the compression level currently used.
     */
    public int getLevel() {
        return def.getLevel();
    }

    /**
     * Sets the compression level for subsequent entries.
     */
    public void setLevel(int level) {
	def.setLevel(level);
    }

    /**
     * Returns the default compression method for subsequent entries.
     * This property is only used if a {@link ZipEntry} does not specify a
     * compression method.
     *
     * @see #setMethod
     * @see ZipEntry#getMethod
     */
    public int getMethod() {
        return method;
    }

    /**
     * Sets the default compression method for subsequent entries.
     * This property is only used if a {@link ZipEntry} does not specify a
     * compression method.
     * <p>
     * Legal values are {@link ZipEntry#STORED} (uncompressed) and
     * {@link ZipEntry#DEFLATED} (compressed).
     * The initial value is {@link ZipEntry#DEFLATED}.
     *
     * @see #getMethod
     * @see ZipEntry#setMethod
     */
    public void setMethod(int method) {
	if (method != ZIP.STORED && method != ZIP.DEFLATED)
	    throw new IllegalArgumentException(
                    "Invalid compression method: " + method);
        this.method = (short) method;
    }

    /**
     * Returns the total number of (compressed) bytes this stream has written
     * to the underlying stream.
     */
    public long length() {
        return ((LEDataOutputStream) out).size();
    }

    /**
     * Returns <code>true</code> if and only if this
     * <code>BasicZipOutputStream</code> is currently writing a ZIP entry.
     */
    public boolean isBusy() {
        return entry != null;
    }

    /**
     * Equivalent to
     * {@link #putNextEntry(ZipEntry, boolean) putNextEntry(entry, true)}.
     */
    public final void putNextEntry(final ZipEntry entry)
    throws IOException {
        putNextEntry(entry, true);
    }

    /**
     * Starts writing the next ZIP entry to the underlying stream.
     * Note that if two or more entries with the same name are written
     * consecutively to this stream, the last entry written will shadow
     * all other entries, i.e. all of them are written to the ZIP compatible
     * file (and hence require space), but only the last will be accessible
     * from the central directory.
     * This is unlike the genuine {@link java.util.zip.ZipOutputStream
     * java.util.zip.ZipOutputStream} which would throw a {@link ZipException}
     * in this method when the second entry with the same name is to be written.
     *
     * @param entry The ZIP entry to write.
     * @param deflate Whether or not the entry data should be deflated.
     *        This should be set to {@code false} if and only if you are
     *        writing data which has been read from a ZIP archive file and
     *        has not been inflated again.
     *        The entries' properties CRC, compressed size and uncompressed
     *        size must be set appropriately.
     * @throws ZipException If and only if writing the entry is impossible
     *         because the resulting file would not comply to the ZIP file
     *         format specification.
     * @throws IOException On any I/O related issue.
     */
    public void putNextEntry(final ZipEntry entry, final boolean deflate)
    throws IOException {
        closeEntry();

        final String name = entry.getName();
        /*if (entries.get(name) != null)
            throw new ZipException(name + " (duplicate entry)");*/

        {
            final long size = entry.getNameLength(charset)
                            + entry.getExtraLength()
                            + entry.getCommentLength(charset);
            if (size > UShort.MAX_VALUE)
                throw new ZipException(entry.getName()
                + ": sum of name, extra fields and comment too long: " + size);
        }

        int method = entry.getMethod();
        if (method == ZipEntry.UNKNOWN)
            method = getMethod();
        switch (method) {
            case ZIP.STORED:
                checkLocalFileHeaderData(entry);
                this.deflate = false;
                break;

            case ZIP.DEFLATED:
                if (!deflate)
                    checkLocalFileHeaderData(entry);
                this.deflate = deflate;
                break;

            default:
                throw new ZipException(entry.getName()
                + ": unsupported compression method: " + method);
        }

        if (entry.getPlatform() == ZipEntry.UNKNOWN)
            entry.setPlatform(ZIP.PLATFORM_FAT);
        if (entry.getMethod()   == ZipEntry.UNKNOWN)
            entry.setMethod(method);
        if (entry.getTime()     == ZipEntry.UNKNOWN)
            entry.setTime(System.currentTimeMillis());

        // Write LFH BEFORE putting the entry in the map.
        this.entry = entry;
        writeLocalFileHeader();

        // Store entry now so that an immediate subsequent call to getEntry(...)
        // returns it.
        final ZipEntry old = (ZipEntry) entries.put(name, entry);
        assert old == null;
    }

    private static void checkLocalFileHeaderData(final ZipEntry entry)
    throws ZipException {
        if (entry.getCrc()              == ZipEntry.UNKNOWN)
            throw new ZipException("Unknown CRC Checksum!");
        if (entry.getCompressedSize32() == ZipEntry.UNKNOWN)
            throw new ZipException("Unknown Compressed Size!");
        if (entry.getSize32()           == ZipEntry.UNKNOWN)
            throw new ZipException("Unknown Uncompressed Size!");
    }

    /** @throws IOException On any I/O related issue. */
    private void writeLocalFileHeader() throws IOException {
        assert entry != null;

        final ZipEntry entry = this.entry;
        final LEDataOutputStream dos = (LEDataOutputStream) out;
        final long crc = entry.getCrc();
        final long csize = entry.getCompressedSize();
        final long size = entry.getSize();
        final long csize32 = entry.getCompressedSize32();
        final long size32 = entry.getSize32();
        final long offset = dos.size();
        final boolean dd // data descriptor?
                =  crc   == ZipEntry.UNKNOWN
                || csize == ZipEntry.UNKNOWN
                || size  == ZipEntry.UNKNOWN;
        final boolean zip64 // ZIP64 extensions?
                =  csize  >= UInt.MAX_VALUE
                || size   >= UInt.MAX_VALUE
                || offset >= UInt.MAX_VALUE
                || ZIP.ZIP64_EXT;

        // Compose General Purpose Bit Flag.
        // See appendix D of PKWARE's ZIP File Format Specification.
        final boolean utf8 = ZIP.UTF8.equalsIgnoreCase(charset);
        final int general = (dd   ? (1 <<  3) : 0)
                          | (utf8 ? (1 << 11) : 0);

        // Start changes.
        finished = false;

        // Local File Header Signature.
        dos.writeInt(ZIP.LFH_SIG);

        // Version Needed To Extract.
        dos.writeShort(zip64 ? 45 : dd ? 20 : 10);

        // General Purpose Bit Flag.
        dos.writeShort(general);

        // Compression Method.
        dos.writeShort(entry.getMethod());

        // Last Mod. Time / Date in DOS format.
        dos.writeInt((int) entry.getDosTime());

        // CRC-32.
        // Compressed Size.
        // Uncompressed Size.
        if (dd) {
            dos.writeInt(0);
            dos.writeInt(0);
            dos.writeInt(0);
        } else {
            dos.writeInt((int) crc);
            dos.writeInt((int) csize32);
            dos.writeInt((int) size32);
        }

        // File Name Length.
        final byte[] name = entry.getName().getBytes(charset);
        dos.writeShort(name.length);

        // Extra Field Length.
        final byte[] extra = entry.getExtra(!dd);
        assert extra != null;
        dos.writeShort(extra.length);

        // File Name.
        dos.write(name);

        // Extra Field(s).
        dos.write(extra);

        // Commit changes.
        entry.setGeneral(general);
        entry.setOffset(offset);
        dataStart = dos.size();
    }

    /**
     * @throws IOException On any I/O related issue.
     */
    public void write(int b) throws IOException {
        byte[] buf = sbuf;
        buf[0] = (byte) b;
        write(buf, 0, 1);
    }

    /**
     * @throws IOException On any I/O related issue.
     */
    public void write(final byte[] b, final int off, final int len)
    throws IOException {
        if (entry != null) {
            if (len == 0) // let negative values pass for an exception
                return;
            if (deflate) {
                // Fast implementation.
                assert !def.finished();
                def.setInput(b, off, len);
                while (!def.needsInput())
                    deflate();
                crc.update(b, off, len);
            } else {
                out.write(b, off, len);
                if (entry.getMethod() != ZIP.DEFLATED)
                    crc.update(b, off, len);
            }
        } else {
            out.write(b, off, len);
        }
    }

    private final void deflate() throws IOException {
        final int dlen = def.deflate(dbuf, 0, dbuf.length);
        if (dlen > 0)
            out.write(dbuf, 0, dlen);
    }

    /**
     * Writes all necessary data for this entry to the underlying stream.
     *
     * @throws ZipException If and only if writing the entry is impossible
     *         because the resulting file would not comply to the ZIP file
     *         format specification.
     * @throws IOException On any I/O related issue.
     */
    public void closeEntry() throws IOException {
        if (entry == null)
            return;

        switch (entry.getMethod()) {
            case ZIP.STORED:
                final long expectedCrc = crc.getValue();
                if (expectedCrc != entry.getCrc()) {
                    throw new ZipException(entry.getName()
                    + ": bad CRC-32: "
                    + Long.toHexString(entry.getCrc())
                    + " expected: "
                    + Long.toHexString(expectedCrc));
                }
                final long written = ((LEDataOutputStream) out).size();
                final long entrySize = written - dataStart;
                if (entry.getSize() != entrySize) {
                    throw new ZipException(entry.getName()
                    + ": bad Uncompressed Size: "
                    + entry.getSize()
                    + " expected: "
                    + entrySize);
                }
                break;

            case ZIP.DEFLATED:
                if (deflate) {
                    assert !def.finished();
                    def.finish();
                    while (!def.finished())
                        deflate();

                    entry.setCrc(crc.getValue());
                    entry.setCompressedSize(def.getBytesWritten());
                    entry.setSize(def.getBytesRead());

                    def.reset();
                } else {
                    // Note: There is no way to check whether the written
                    // data matches the crc, the compressed size and the
                    // uncompressed size!
                }
                break;

            default:
                throw new ZipException(entry.getName()
                + ": unsupported Compression Method: "
                + entry.getMethod());
        }

        writeDataDescriptor();
        flush();
        crc.reset();
        entry = null;
    }

    /**
     * @throws IOException On any I/O related issue.
     */
    private void writeDataDescriptor() throws IOException {
        final ZipEntry entry = this.entry;
        assert entry != null;

        if (!entry.getGeneralBit(3))
            return;

        final LEDataOutputStream dos = (LEDataOutputStream) out;
        final long crc = entry.getCrc();
        final long csize = entry.getCompressedSize();
        final long size = entry.getSize();
        final long offset = entry.getOffset();
        // Offset MUST be considered in decision about ZIP64 format - see
        // description of Data Descriptor in ZIP File Format Specification!
        final boolean zip64 // ZIP64 extensions?
                =  csize  >= UInt.MAX_VALUE
                || size   >= UInt.MAX_VALUE
                || offset >= UInt.MAX_VALUE
                || ZIP.ZIP64_EXT;

        // Data Descriptor Signature.
        dos.writeInt(ZIP.DD_SIG);

        // CRC-32.
        dos.writeInt((int) crc);

        // Compressed Size.
        // Uncompressed Size.
        if (zip64) {
            dos.writeLong(csize);
            dos.writeLong(size);
        } else {
            dos.writeInt((int) csize);
            dos.writeInt((int) size);
        }
    }

    /**
     * Closes the current entry and writes the Central Directory to the
     * underlying output stream.
     * <p>
     * <b>Notes:</b>
     * <ul>
     * <li>The underlying stream is not closed.</li>
     * <li>Unlike Sun's implementation in J2SE 1.4.2, you may continue to use
     *     this ZIP output stream with putNextEntry(...) and the like.
     *     When you finally close the stream, the central directory will
     *     contain <em>all</em> entries written.</li>
     * </ul>
     *
     * @throws ZipException If and only if writing the entry is impossible
     *         because the resulting file would not comply to the ZIP file
     *         format specification.
     * @throws IOException On any I/O related issue.
     */
    public void finish() throws IOException {
        if (finished)
            return;

        // Order is important here!
        finished = true;
        closeEntry();
        final LEDataOutputStream dos = (LEDataOutputStream) out;
        cdOffset = dos.size();
        for (final Iterator i = entries.values().iterator(); i.hasNext(); )
            writeCentralFileHeader((ZipEntry) i.next());
        writeEndOfCentralDirectory();
    }

    /**
     * Writes a Central File Header record.
     *
     * @throws IOException On any I/O related issue.
     */
    private void writeCentralFileHeader(final ZipEntry entry) throws IOException {
        assert entry != null;

        final LEDataOutputStream dos = (LEDataOutputStream) out;
        final long csize32 = entry.getCompressedSize32();
        final long size32 = entry.getSize32();
        final long offset32 = entry.getOffset32();
        final boolean dd = entry.getGeneralBit(3);
        final boolean zip64 // ZIP64 extensions?
                =  csize32  >= UInt.MAX_VALUE
                || size32   >= UInt.MAX_VALUE
                || offset32 >= UInt.MAX_VALUE
                || ZIP.ZIP64_EXT;

        // Central File Header.
        dos.writeInt(ZIP.CFH_SIG);

        // Version Made By.
        dos.writeShort((entry.getPlatform() << 8) | 63);

        // Version Needed To Extract.
        dos.writeShort(zip64 ? 45 : dd ? 20 : 10);

        // General Purpose Bit Flag.
        dos.writeShort(entry.getGeneral());

        // Compression Method.
        dos.writeShort(entry.getMethod());

        // Last Mod. File Time / Date.
        dos.writeInt((int) entry.getDosTime());

        // CRC-32.
        // Compressed Size.
        // Uncompressed Size.
        dos.writeInt((int) entry.getCrc());
        dos.writeInt((int) csize32);
        dos.writeInt((int) size32);

        // File Name Length.
        final byte[] name = entry.getName().getBytes(charset);
        dos.writeShort(name.length);

        // Extra Field Length.
        final byte[] extra = entry.getExtra();
        assert extra != null;
        dos.writeShort(extra.length);

        // File Comment Length.
        String comment = entry.getComment();
        if (comment == null)
            comment = "";
        final byte[] data = comment.getBytes(charset);
        dos.writeShort(data.length);

        // Disk Number Start.
        dos.writeShort(0);

        // Internal File Attributes.
        dos.writeShort(0);

        // External File Attributes.
        dos.writeInt(entry.isDirectory() ? 0x10 : 0); // fixed issue #27.

        // Relative Offset Of Local File Header.
        dos.writeInt((int) offset32);

        // File Name.
        dos.write(name);

        // Extra Field(s).
        dos.write(extra);

        // File Comment.
        dos.write(data);
    }

    /**
     * Writes the End Of Central Directory record.
     *
     * @throws IOException On any I/O related issue.
     */
    private void writeEndOfCentralDirectory() throws IOException {
        final LEDataOutputStream dos = (LEDataOutputStream) out;
        final long cdEntries = entries.size();
        final long cdSize = dos.size() - cdOffset;
        final long cdOffset = this.cdOffset;
        final boolean cdEntriesZip64 = cdEntries > UShort.MAX_VALUE || ZIP.ZIP64_EXT;
        final boolean cdSizeZip64    = cdSize    > UInt  .MAX_VALUE || ZIP.ZIP64_EXT;
        final boolean cdOffsetZip64  = cdOffset  > UInt  .MAX_VALUE || ZIP.ZIP64_EXT;
        final int cdEntries16 = cdEntriesZip64 ? UShort.MAX_VALUE : (int) cdEntries;
        final long cdSize32   = cdSizeZip64    ? UInt  .MAX_VALUE : cdSize;
        final long cdOffset32 = cdOffsetZip64  ? UInt  .MAX_VALUE : cdOffset;
        final boolean zip64 // ZIP64 extensions?
                =  cdEntriesZip64
                || cdSizeZip64
                || cdOffsetZip64;

        if (zip64) {
            final long zip64eocdrOffset // relative offset of the zip64 end of central directory record
                    = dos.size();

            // ZIP64 End Of Central Directory Record Signature.
            dos.writeInt(ZIP.ZIP64_EOCDR_SIG);

            // Size Of ZIP64 End Of Central Directory Record.
            dos.writeLong(ZIP.ZIP64_EOCDR_MIN_LEN - 12);

            // Version Made By.
            dos.writeShort(63);

            // Version Needed To Extract.
            dos.writeShort(45);

            // Number Of This Disk.
            dos.writeInt(0);

            // Number Of The Disk With The Start Of The Central Directory.
            dos.writeInt(0);

            // Total Number Of Entries In The Central Directory On This Disk.
            dos.writeLong(cdEntries);

            // Total Number Of Entries In The Central Directory.
            dos.writeLong(cdEntries);

            // Size Of The Central Directory.
            dos.writeLong(cdSize);

            // Offset Of Start Of Central Directory With Respect To The
            // Starting Disk Number.
            dos.writeLong(cdOffset);

            // ZIP64 End Of Central Directory Locator Signature.
            dos.writeInt(ZIP.ZIP64_EOCDL_SIG);

            // Number Of The Disk With The Start Of The ZIP64 End Of Central Directory.
            dos.writeInt(0);

            // Relative Offset Of The ZIP64 End Of Central Directory Record.
            dos.writeLong(zip64eocdrOffset);

            // Total Number Of Disks.
            dos.writeInt(1);
        }

        // End Of Central Directory Record Signature.
        dos.writeInt(ZIP.EOCDR_SIG);

        // Disk numbers.
        dos.writeShort(0);
        dos.writeShort(0);

        // Number of entries.
        dos.writeShort(cdEntries16);
        dos.writeShort(cdEntries16);

        // Length and offset of Central Directory.
        dos.writeInt((int) cdSize32);
        dos.writeInt((int) cdOffset32);

        // ZIP file comment.
        String comment = getComment();
        if (comment == null)
            comment = "";
        final byte[] data = comment.getBytes(charset);
        dos.writeShort(data.length);
        dos.write(data);
    }

    /**
     * Closes this output stream and releases any system resources
     * associated with the stream.
     * This closes the open output stream writing to this ZIP file,
     * if any.
     *
     * @throws IOException On any I/O related issue.
     */
    public void close() throws IOException {
        if (closed)
            return;

        // Order is important here!
        closed = true;
        try {
            finish();
        } finally {
            entries.clear();
            super.close();
        }
    }

    /**
     * A Deflater which can be asked for its current deflation level and
     * counts input and output data length as a long integer value.
     */
    private static class ZipDeflater extends Deflater {
        private int level = Deflater.DEFAULT_COMPRESSION;
        private long read = 0, written = 0;

        public ZipDeflater() {
            super(Deflater.DEFAULT_COMPRESSION, true);
        }

        public void setInput(byte[] b, int off, int len) {
            super.setInput(b, off, len);
            read += len;
        }

        public int getLevel() {
            return level;
        }

        public void setLevel(int level) {
            super.setLevel(level);
            this.level = level;
        }

        public int deflate(byte[] b, int off, int len) {
            int dlen = super.deflate(b, off, len);
            written += dlen;
            return dlen;
        }

        /**
         * Returns the total number of uncompressed bytes input so far.
         */
        public long getBytesRead() {
            return read;
        }

        /**
         * Returns the total number of compressed bytes output so far.
         */
        public long getBytesWritten() {
            return written;
        }

        public void reset() {
            super.reset();
            read = written = 0;
        }
    }
}
