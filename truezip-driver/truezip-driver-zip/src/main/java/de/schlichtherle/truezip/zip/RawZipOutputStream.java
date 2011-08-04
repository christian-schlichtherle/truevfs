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
package de.schlichtherle.truezip.zip;

import de.schlichtherle.truezip.io.DecoratingOutputStream;
import de.schlichtherle.truezip.io.LEDataOutputStream;
import de.schlichtherle.truezip.util.JSE7;
import static de.schlichtherle.truezip.zip.ZipConstants.*;
import static de.schlichtherle.truezip.zip.ZipEntry.*;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.ZipException;
import net.jcip.annotations.NotThreadSafe;

/**
 * Provides unsafe (raw) access to a ZIP file using unsynchronized
 * methods and shared {@link ZipEntry} instances.
 * <p>
 * <b>Warning:</b> This class is <em>not</em> intended for public use
 * - its API may change at will without prior notification!
 *
 * @see     RawZipFile
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
@DefaultAnnotation(NonNull.class)
public abstract class RawZipOutputStream<E extends ZipEntry>
extends DecoratingOutputStream
implements Iterable<E> {

    /**
     * The default character set used for entry names and comments in ZIP files.
     * This is {@code "UTF-8"} for compatibility with Sun's JDK implementation.
     */
    public static final Charset DEFAULT_CHARSET = ZipConstants.DEFAULT_CHARSET;

    /** The charset to use for entry names and comments. */
    private final Charset charset;

    /** This instance is used for deflated output. */
    private final ZipDeflater def = JSE7.AVAILABLE
            ? new ZipDeflater()
            : new Jdk6Deflater();

    /** The encoded file comment. */
    private @CheckForNull byte[] comment;

    /** Default compression method for next entry. */
    private short method = DEFLATED;

    /**
     * The list of ZIP entries started to be written so far.
     * Maps entry names to zip entries.
     */
    private final Map<String, E> entries = new LinkedHashMap<String, E>();

    /** Start of entry data. */
    private long dataStart;

    /** Start of central directory. */
    private long cdOffset;

    private boolean finished;

    private boolean closed;

    /** Current entry. */
    private @CheckForNull E entry;

    private @CheckForNull ZipCryptoParameters cryptoParameters;

    /**
     * Constructs a raw ZIP output stream which decorates the given output
     * stream using the given charset.
     *
     * @throws UnsupportedCharsetException If {@code charset} is not supported
     *         by this JVM.
     */
    protected RawZipOutputStream(
            final OutputStream out,
            final Charset charset) {
        super(promote(out, null));
        if (null == charset)
            throw new NullPointerException();
        this.charset = charset;
    }

    /**
     * Constructs a raw ZIP output stream which decorates the given output
     * stream and appends to the given raw ZIP file.
     *
     * @param  out The output stream to write the ZIP file to.
     *         If {@code appendee} is not {@code null}, then this must be set
     *         up so that it appends to the same ZIP file from which
     *         {@code appendee} is reading.
     * @param  appendee the raw ZIP file to append to.
     *         This may already be closed.
     */
    protected RawZipOutputStream(
            final OutputStream out,
            final RawZipFile<E> appendee) {
        this(out, appendee, null);
    }

    /**
     * Constructs a raw ZIP output stream which decorates the given output
     * stream and optionally apppends to the given raw ZIP file.
     * <p>
     * This constructor is not intended for ordinary use.
     *
     * @param  out The output stream to write the ZIP file to.
     *         If {@code appendee} is not {@code null}, then this must be set
     *         up so that it appends to the same ZIP file from which
     *         {@code appendee} is reading.
     * @param  appendee the raw ZIP file to append to.
     *         This may already be closed.
     * @param  charset the character set to use if {@code appendee} is
     *         {@code null}.
     * @since  TrueZIP 7.3
     */
    protected RawZipOutputStream(
            final OutputStream out,
            final @CheckForNull RawZipFile<E> appendee,
            final @CheckForNull Charset charset) {
        super(promote(out, appendee));
        if (null != appendee) {
            this.charset = appendee.getFileCharset();
            this.comment = appendee.getFileComment();
            final Map<String, E> entries = this.entries;
            for (E entry : appendee)
                entries.put(entry.getName(), entry);
        } else {
            if (null == charset)
                throw new NullPointerException();
            this.charset = charset;
        }
    }

    private static LEDataOutputStream promote(
            final OutputStream out,
            final @CheckForNull RawZipFile<?> appendee) {
        if (null == out)
            throw new NullPointerException();
        return null != appendee
                ? new AppendingLEDataOutputStream(out, appendee)
                : out instanceof LEDataOutputStream
                    ? (LEDataOutputStream) out
                    : new LEDataOutputStream(out);
    }

    /* Adjusts the number of written bytes for appending mode. */
    private static final class AppendingLEDataOutputStream
    extends LEDataOutputStream {
        AppendingLEDataOutputStream(OutputStream out, RawZipFile<?> appendee) {
            super(out);
            super.written = appendee.getOffsetMapper().location(appendee.length());
        }
    } // AppendingLEDataOutputStream

    private byte[] encode(String string) {
        return string.getBytes(charset);
    }

    private String decode(byte[] bytes) {
        return new String(bytes, charset);
    }

    /** Returns the charset to use for entry names and the file comment. */
    public String getCharset() {
        return charset.name();
    }

    /**
     * Returns the number of ZIP entries written so far.
     */
    public int size() {
        return entries.size();
    }

    /**
     * Returns an enumeration of all entries written to this ZIP file
     * so far.
     * Note that the enumerated entries are shared with this class.
     * It is illegal to put more entries into this ZIP output stream
     * concurrently or modify the state of the enumerated entries.
     *
     * @deprecated Use {@link #iterator()} instead.
     */
    @Deprecated
    public Enumeration<? extends ZipEntry> entries() {
        return Collections.enumeration(entries.values());
    }

    /**
     * Returns an iteration of all entries written to this ZIP file so
     * far.
     * Note that the iteration supports element removal and the returned
     * entries are shared with this instance.
     * It is illegal to put more entries into this ZIP output stream
     * concurrently or modify the state of the iterated entries.
     */
    @Override
    public Iterator<E> iterator() {
        return entries.values().iterator();
    }

    /**
     * Returns the entry for the given name or {@code null} if no entry with
     * this name exists.
     * Note that the returned entry is shared with this instance.
     * It is illegal to change its state!
     *
     * @param name the name of the ZIP entry.
     */
    public E getEntry(String name) {
        return entries.get(name);
    }

    /**
     * Returns the file comment.
     * 
     * @return The file comment.
     */
    public @Nullable String getComment() {
        final byte[] comment = this.comment;
        //return null == comment ? null : new String(comment, charset);
        return null == comment ? null : decode(comment);
    }

    /**
     * Sets the file comment.
     * 
     * @param  comment the file comment.
     * @throws IllegalArgumentException if the encoded comment is longer than
     *         {@value UShort#MAX_VALUE} bytes.
     */
    public void setComment(final @CheckForNull String comment) {
        if (null != comment && !comment.isEmpty()) {
            //final byte[] bytes = comment.getBytes(charset);
            final byte[] bytes = encode(comment);
            UShort.check(bytes.length);
            this.comment = bytes;
        } else {
            this.comment = null;
        }
    }

    /**
     * Returns the default compression method for subsequent entries.
     * This property is only used if a {@link ZipEntry} does not specify a
     * compression method.
     * <p>
     * The initial value is {@link ZipEntry#DEFLATED}.
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
     *
     * @see #getMethod
     * @see ZipEntry#setMethod
     */
    public void setMethod(final int method) {
	if (STORED != method && DEFLATED != method)
	    throw new IllegalArgumentException(
                    "Invalid compression method: " + method);
        this.method = (short) method;
    }

    /**
     * Returns the current compression level.
     * 
     * @return The current compression level.
     */
    public int getLevel() {
        return def.getLevel();
    }

    /**
     * Sets the compression level for subsequent entries.
     * 
     * @param level the compression level.
     * @throws IllegalArgumentException if the compression level is invalid.
     */
    public void setLevel(int level) {
	def.setLevel(level);
    }

    /**
     * Returns the crypto parameters.
     * 
     * @return The crypto parameters.
     */
    /*public @Nullable ZipCryptoParameters getCryptoParameters() {
        return cryptoParameters;
    }*/

    /**
     * Sets the crypto parameters.
     * 
     * @param cryptoParameters the crypto parameters.
     */
    /*public void setCryptoParameters(final @CheckForNull ZipCryptoParameters cryptoParameters) {
        this.cryptoParameters = cryptoParameters;
    }*/

    /**
     * Returns the total number of (compressed) bytes this stream has written
     * to the underlying stream.
     */
    public long length() {
        return ((LEDataOutputStream) delegate).size();
    }

    /**
     * Returns {@code true} if and only if this
     * {@code RawZipOutputStream} is currently writing a ZIP entry.
     */
    public boolean isBusy() {
        return null != entry;
    }

    /**
     * Equivalent to
     * {@link #putNextEntry(ZipEntry, boolean) putNextEntry(entry, true)}.
     */
    public final void putNextEntry(final E entry) throws IOException {
        putNextEntry(entry, true);
    }

    /**
     * Starts writing the next ZIP entry to the underlying stream.
     * Note that if two or more entries with the same name are written
     * consecutively to this stream, the last entry written will shadow
     * all other entries, i.e. all of them are written to the ZIP file
     * (and hence require space), but only the last will be accessible from
     * the central directory.
     * This is unlike the genuine {@link java.util.zip.ZipOutputStream
     * java.util.zip.ZipOutputStream} which would throw a {@link ZipException}
     * in this method when the second entry with the same name is to be written.
     *
     * @param  entry The ZIP entry to write.
     * @param  deflate Whether or not the entry data should get deflated.
     *         This should be set to {@code false} if and only if you are
     *         writing data which has been read from a ZIP file and has not
     *         been inflated again.
     *         The entries' properties CRC, compressed size and uncompressed
     *         size must be set appropriately.
     * @throws ZipException If and only if writing the entry is impossible
     *         because the resulting file would not comply to the ZIP file
     *         format specification.
     * @throws IOException On any I/O error.
     */
    public void putNextEntry(final E entry, final boolean deflate)
    throws IOException {
        closeEntry();
        setupEntry(entry, deflate);
        final OutputStream out = openOutput(entry, deflate);
        this.entry = entry;
        // Write LFH BEFORE putting the entry in the map.
        writeLocalFileHeader();
        // Store entry now so that a subsequent call to getEntry(...) returns
        // it.
        entries.put(entry.getName(), entry);
        this.delegate = out;
    }

    /**
     * This method may have side effects on {@code entry} only.
     */
    private void setupEntry(final E entry, final boolean deflate)
    throws IOException {
        {
            final long size = entry.getNameLength(charset)
                            + entry.getExtraLength()
                            + entry.getCommentLength(charset);
            if (size > UShort.MAX_VALUE)
                throw new ZipException(entry.getName()
                + " (sum of name, extra fields and comment is too long: " + size + ")");
        }
        int method = entry.getMethod();
        if (UNKNOWN == method)
            entry.setMethod(method = getMethod());
        switch (method) {
            case STORED:
                assert deflate;
                checkLocalFileHeaderData(entry);
                break;
            case DEFLATED:
                if (!deflate)
                    checkLocalFileHeaderData(entry);
                break;
            default:
                throw new ZipException(entry.getName()
                + " (unsupported compression method: " + method + ")");
        }
        if (UNKNOWN == entry.getPlatform())
            entry.setPlatform(PLATFORM_FAT);
        if (UNKNOWN == entry.getTime())
            entry.setTime(System.currentTimeMillis());
    }

    private static void checkLocalFileHeaderData(final ZipEntry entry)
    throws IOException {
        if (UNKNOWN == entry.getCrc())
            throw new ZipException(entry.getName() + " (unknown CRC checksum)");
        if (UNKNOWN == entry.getCompressedSize32())
            throw new ZipException(entry.getName() + " (unknown compressed size)");
        if (UNKNOWN == entry.getSize32())
            throw new ZipException(entry.getName() + " (unknown uncompressed size)");
    }

    /**
     * This method may not have any side effects.
     */
    private OutputStream openOutput(final E entry, final boolean deflate)
    throws IOException {
        switch (entry.getMethod()) {
            case STORED:
                return new ZipStoredOutputStream(
                        new NeverClosingOutputStream(this.delegate));
            case DEFLATED:
                if (deflate) {
                    return new ZipDeflatedOutputStream(
                            new ZipDeflaterOutputStream(this.delegate));
                } else {
                    assert UNKNOWN != entry.getCrc();
                    return new NeverClosingOutputStream(this.delegate);
                }
            default:
                throw new AssertionError();
        }
    }

    /** @throws IOException On any I/O error. */
    private void writeLocalFileHeader()
    throws IOException {
        final E entry = this.entry;
        assert null != entry;
        final LEDataOutputStream out = (LEDataOutputStream) this.delegate;
        final long crc = entry.getCrc();
        final long csize = entry.getCompressedSize();
        final long size = entry.getSize();
        final long csize32 = entry.getCompressedSize32();
        final long size32 = entry.getSize32();
        final long offset = out.size();
        final boolean encrypted = entry.isEncrypted();
        final boolean dd // data descriptor?
                =  UNKNOWN == crc
                || UNKNOWN == csize
                || UNKNOWN == size;
        final boolean zip64 // ZIP64 extensions?
                =  UInt.MAX_VALUE <= csize
                || UInt.MAX_VALUE <= size
                || UInt.MAX_VALUE <= offset
                || FORCE_ZIP64_EXT;
        // Compose General Purpose Bit Flag.
        // See appendix D of PKWARE's ZIP File Format Specification.
        final boolean utf8 = UTF8.equals(charset);
        final int general = (encrypted ? (1 << GPBF_ENCRYPTED) : 0)
                          | (dd        ? (1 << GPBF_DATA_DESCRIPTOR) : 0)
                          | (utf8      ? (1 << GPBF_UTF8) : 0);
        // Start changes.
        this.finished = false;
        // Local File Header Signature.
        out.writeInt(LFH_SIG);
        // Version Needed To Extract.
        out.writeShort(zip64 ? 45 : dd ? 20 : 10);
        // General Purpose Bit Flag.
        out.writeShort(general);
        // Compression Method.
        out.writeShort(entry.getMethod());
        // Last Mod. Time / Date in DOS format.
        out.writeInt((int) entry.getDosTime());
        // CRC-32.
        // Compressed Size.
        // Uncompressed Size.
        if (dd) {
            out.writeInt(0);
            out.writeInt(0);
            out.writeInt(0);
        } else {
            out.writeInt((int) crc);
            out.writeInt((int) csize32);
            out.writeInt((int) size32);
        }
        // File Name Length.
        final byte[] name = encode(entry.getName());
        out.writeShort(name.length);
        // Extra Field Length.
        final byte[] extra = entry.getExtra(!dd);
        assert extra != null;
        out.writeShort(extra.length);
        // File Name.
        out.write(name);
        // Extra Field(s).
        out.write(extra);
        // Commit changes.
        entry.setGeneral(general);
        entry.setOffset(offset);
        this.dataStart = out.size();
    }

    /**
     * Mind the side effects!
     */
    private void closeOutput() throws IOException {
        final OutputStream out = ((ProxyOutputStream) delegate)
                .getOriginalOutputStream();
        assert out instanceof LEDataOutputStream;
        try {
            this.delegate.close();
        } finally {
            this.delegate = out;
        }
    }

    /**
     * Writes all necessary data for this entry to the underlying stream.
     *
     * @throws ZipException If and only if writing the entry is impossible
     *         because the resulting file would not comply to the ZIP file
     *         format specification.
     * @throws IOException On any I/O error.
     */
    public void closeEntry() throws IOException {
        final E entry = this.entry;
        if (null == entry)
            return;
        closeOutput();
        writeDataDescriptor();
        flush();
        this.entry = null;
    }

    /**
     * @throws IOException On any I/O error.
     */
    private void writeDataDescriptor() throws IOException {
        final E entry = this.entry;
        assert null != entry;
        if (!entry.getGeneralBit(GPBF_DATA_DESCRIPTOR))
            return;
        final LEDataOutputStream out = (LEDataOutputStream) this.delegate;
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
                || FORCE_ZIP64_EXT;
        // Data Descriptor Signature.
        out.writeInt(DD_SIG);
        // CRC-32.
        out.writeInt((int) crc);
        // Compressed Size.
        // Uncompressed Size.
        if (zip64) {
            out.writeLong(csize);
            out.writeLong(size);
        } else {
            out.writeInt((int) csize);
            out.writeInt((int) size);
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
     * @throws IOException On any I/O error.
     */
    public void finish() throws IOException {
        if (this.finished)
            return;
        this.finished = true;
        closeEntry();
        final LEDataOutputStream out = (LEDataOutputStream) this.delegate;
        this.cdOffset = out.size();
        for (E entry : this.entries.values())
            writeCentralFileHeader(entry);
        writeEndOfCentralDirectory();
    }

    /**
     * Writes a Central File Header record.
     *
     * @throws IOException On any I/O error.
     */
    private void writeCentralFileHeader(final E entry) throws IOException {
        assert null != entry;
        final LEDataOutputStream out = (LEDataOutputStream) this.delegate;
        final long csize32 = entry.getCompressedSize32();
        final long size32 = entry.getSize32();
        final long offset32 = entry.getOffset32();
        final boolean dd = entry.getGeneralBit(GPBF_DATA_DESCRIPTOR);
        final boolean zip64 // ZIP64 extensions?
                =  csize32  >= UInt.MAX_VALUE
                || size32   >= UInt.MAX_VALUE
                || offset32 >= UInt.MAX_VALUE
                || FORCE_ZIP64_EXT;
        // Central File Header.
        out.writeInt(CFH_SIG);
        // Version Made By.
        out.writeShort((entry.getPlatform() << 8) | 63);
        // Version Needed To Extract.
        out.writeShort(zip64 ? 45 : dd ? 20 : 10);
        // General Purpose Bit Flags.
        out.writeShort(entry.getGeneral());
        // Compression Method.
        out.writeShort(entry.getMethod());
        // Last Mod. File Time / Date.
        out.writeInt((int) entry.getDosTime());
        // CRC-32.
        out.writeInt((int) entry.getCrc());
        // Compressed Size.
        out.writeInt((int) csize32);
        // Uncompressed Size.
        out.writeInt((int) size32);
        // File Name Length.
        final byte[] name = encode(entry.getName());
        out.writeShort(name.length);
        // Extra Field Length.
        final byte[] extra = entry.getExtra();
        assert extra != null;
        out.writeShort(extra.length);
        // File Comment Length.
        final byte[] comment = getEntryComment(entry);
        out.writeShort(comment.length);
        // Disk Number Start.
        out.writeShort(0);
        // Internal File Attributes.
        out.writeShort(0);
        // External File Attributes.
        out.writeInt(entry.isDirectory() ? 0x10 : 0); // fixed issue #27.
        // Relative Offset Of Local File Header.
        out.writeInt((int) offset32);
        // File Name.
        out.write(name);
        // Extra Field(s).
        out.write(extra);
        // File Comment.
        out.write(comment);
    }

    private byte[] getEntryComment(final ZipEntry entry) {
        String comment = entry.getComment();
        if (comment == null)
            comment = "";
        return encode(comment);
    }

    /**
     * Writes the End Of Central Directory record.
     *
     * @throws IOException On any I/O error.
     */
    private void writeEndOfCentralDirectory() throws IOException {
        final LEDataOutputStream out = (LEDataOutputStream) this.delegate;
        final long cdEntries = entries.size();
        final long cdSize = out.size() - cdOffset;
        final long cdOffset = this.cdOffset;
        final boolean cdEntriesZip64 = cdEntries > UShort.MAX_VALUE || FORCE_ZIP64_EXT;
        final boolean cdSizeZip64    = cdSize    > UInt  .MAX_VALUE || FORCE_ZIP64_EXT;
        final boolean cdOffsetZip64  = cdOffset  > UInt  .MAX_VALUE || FORCE_ZIP64_EXT;
        final int cdEntries16 = cdEntriesZip64 ? UShort.MAX_VALUE : (int) cdEntries;
        final long cdSize32   = cdSizeZip64    ? UInt  .MAX_VALUE : cdSize;
        final long cdOffset32 = cdOffsetZip64  ? UInt  .MAX_VALUE : cdOffset;
        final boolean zip64 // ZIP64 extensions?
                =  cdEntriesZip64
                || cdSizeZip64
                || cdOffsetZip64;
        if (zip64) {
            final long zip64eocdOffset // relative offset of the zip64 end of central directory record
                    = out.size();
            // ZIP64 End Of Central Directory Record signature.
            out.writeInt(ZIP64_EOCDR_SIG);
            // Size Of ZIP64 End Of Central Directory Record.
            out.writeLong(ZIP64_EOCDR_MIN_LEN - 12);
            // Version Made By.
            out.writeShort(63);
            // Version Needed To Extract.
            out.writeShort(45);
            // Number Of This Disk.
            out.writeInt(0);
            // Number Of The Disk With The Start Of The Central Directory.
            out.writeInt(0);
            // Total Number Of Entries In The Central Directory On This Disk.
            out.writeLong(cdEntries);
            // Total Number Of Entries In The Central Directory.
            out.writeLong(cdEntries);
            // Size Of The Central Directory.
            out.writeLong(cdSize);
            // Offset Of Start Of Central Directory With Respect To The
            // Starting Disk Number.
            out.writeLong(cdOffset);
            // ZIP64 End Of Central Directory Locator signature.
            out.writeInt(ZIP64_EOCDL_SIG);
            // Number Of The Disk With The Start Of The ZIP64 End Of Central Directory.
            out.writeInt(0);
            // Relative Offset Of The ZIP64 End Of Central Directory record.
            out.writeLong(zip64eocdOffset);
            // Total Number Of Disks.
            out.writeInt(1);
        }
        // End Of Central Directory record signature.
        out.writeInt(EOCDR_SIG);
        // Disk numbers.
        out.writeShort(0);
        out.writeShort(0);
        // Number of entries.
        out.writeShort(cdEntries16);
        out.writeShort(cdEntries16);
        // Length and offset of Central Directory.
        out.writeInt((int) cdSize32);
        out.writeInt((int) cdOffset32);
        // ZIP file comment.
        final byte[] comment = getFileComment();
        out.writeShort(comment.length);
        out.write(comment);
    }

    private byte[] getFileComment() {
        final byte[] comment = this.comment;
        return null != comment ? comment : new byte[0];
    }

    /**
     * Closes this output stream and releases any system resources
     * associated with the stream.
     * This closes the open output stream writing to this ZIP file,
     * if any.
     *
     * @throws IOException On any I/O error.
     */
    @Override
    public void close() throws IOException {
        if (this.closed)
            return;
        this.closed = true;
        try {
            finish();
        } finally {
            this.entries.clear();
            this.delegate.close();
        }
    }

    private interface ProxyOutputStream extends Closeable {
        OutputStream getOriginalOutputStream();
    }

    private final class ZipDeflaterOutputStream
    extends DeflaterOutputStream
    implements ProxyOutputStream {

        ZipDeflaterOutputStream(OutputStream out) {
            super(out, RawZipOutputStream.this.def, FLATER_BUF_LENGTH);
            assert !(out instanceof ProxyOutputStream);
        }

        @Override
        public void close() throws IOException {
            super.finish();
            final E entry = RawZipOutputStream.this.entry;
            final ZipDeflater def = RawZipOutputStream.this.def;
            entry.setCompressedSize(def.getBytesWritten());
            entry.setSize(def.getBytesRead());
            def.reset();
        }

        @Override
        public OutputStream getOriginalOutputStream() {
            return this.out;
        }
    }

    private static final class NeverClosingOutputStream
    extends DecoratingOutputStream
    implements ProxyOutputStream {

        NeverClosingOutputStream(OutputStream out) {
            super(out);
            assert !(out instanceof ProxyOutputStream);
        }

        @Override
        public void close() throws IOException {
            super.flush();
        }

        @Override
        public OutputStream getOriginalOutputStream() {
            return this.delegate;
        }
    }

    private class ZipCrc32OutputStream
    extends CheckedOutputStream
    implements ProxyOutputStream {

        ZipCrc32OutputStream(ProxyOutputStream out) {
            super((OutputStream) out, new CRC32());
        }

        @Override
        public OutputStream getOriginalOutputStream() {
            return ((ProxyOutputStream) this.out).getOriginalOutputStream();
        }
    }

    private class ZipDeflatedOutputStream
    extends ZipCrc32OutputStream {

        ZipDeflatedOutputStream(ProxyOutputStream out) {
            super(out);
        }

        @Override
        public void close() throws IOException {
            super.close();
            final E entry = RawZipOutputStream.this.entry;
            entry.setCrc(getChecksum().getValue());
        }
    }

    private class ZipStoredOutputStream
    extends ZipCrc32OutputStream {

        ZipStoredOutputStream(ProxyOutputStream out) {
            super(out);
        }

        @Override
        public void close() throws IOException {
            super.close();
            final E entry = RawZipOutputStream.this.entry;
            final long expectedCrc = getChecksum().getValue();
            if (expectedCrc != entry.getCrc()) {
                throw new ZipException(entry.getName()
                + " (bad CRC-32: 0x"
                + Long.toHexString(entry.getCrc())
                + "; expected: 0x"
                + Long.toHexString(expectedCrc)
                + ")");
            }
            final long written = ((LEDataOutputStream) getOriginalOutputStream()).size();
            final long entrySize = written - RawZipOutputStream.this.dataStart;
            if (entry.getSize() != entrySize) {
                throw new ZipException(entry.getName()
                + " (bad uncompressed Size: "
                + entry.getSize()
                + "; expected: "
                + entrySize
                + ")");
            }
        }
    }
}
