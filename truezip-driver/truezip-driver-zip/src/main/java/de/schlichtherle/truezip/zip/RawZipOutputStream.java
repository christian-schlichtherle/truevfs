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

import de.schlichtherle.truezip.crypto.param.AesKeyStrength;
import de.schlichtherle.truezip.io.DecoratingOutputStream;
import de.schlichtherle.truezip.io.LEDataOutputStream;
import de.schlichtherle.truezip.util.JSE7;
import static de.schlichtherle.truezip.zip.Constants.*;
import static de.schlichtherle.truezip.zip.WinZipAesExtraField.*;
import static de.schlichtherle.truezip.zip.ZipEntry.*;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
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
    public static final Charset DEFAULT_CHARSET = Constants.DEFAULT_CHARSET;

    private final LEDataOutputStream dos;

    /** The charset to use for entry names and comments. */
    private final Charset charset;

    /** This instance is used for deflated output. */
    private final ZipDeflater deflater = JSE7.AVAILABLE
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

    /** Current ZIP entry. */
    private @Nullable E entry;

    private @Nullable OutputMethod processor;

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
        super(newLEDataOutputStream(out, null));
        if (null == charset)
            throw new NullPointerException();
        this.dos = (LEDataOutputStream) this.delegate;
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
        super(newLEDataOutputStream(out, appendee));
        this.dos = (LEDataOutputStream) this.delegate;
        if (null != appendee) {
            this.charset = appendee.getRawCharset();
            this.comment = appendee.getRawComment();
            final Map<String, E> entries = this.entries;
            for (E entry : appendee)
                entries.put(entry.getName(), entry);
        } else {
            if (null == charset)
                throw new NullPointerException();
            this.charset = charset;
        }
    }

    private static LEDataOutputStream newLEDataOutputStream(
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

    private byte[] encode(String string) {
        return string.getBytes(charset);
    }

    private String decode(byte[] bytes) {
        return new String(bytes, charset);
    }

    /**
     * Returns the character set which is used for
     * encoding entry names and the file comment.
     * 
     * @since TrueZIP 7.3
     */
    public Charset getRawCharset() {
        return charset;
    }

    /**
     * Returns the name of the character set which is used for
     * encoding entry names and the file comment.
     */
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
     *         {@link UShort#MAX_VALUE} bytes.
     */
    public void setComment(final @CheckForNull String comment) {
        if (null != comment && !comment.isEmpty()) {
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
        final ZipEntry test = new ZipEntry("");
        test.setMethod(method);
        this.method = (short) test.getMethod();
    }

    /**
     * Returns the current compression level.
     * 
     * @return The current compression level.
     */
    public int getLevel() {
        return deflater.getLevel();
    }

    /**
     * Sets the compression level for subsequent entries.
     * 
     * @param level the compression level.
     * @throws IllegalArgumentException if the compression level is invalid.
     */
    public void setLevel(int level) {
	deflater.setLevel(level);
    }

    /**
     * Returns the crypto parameters.
     * 
     * @return The crypto parameters.
     * @since  TrueZIP 7.3
     */
    protected abstract @CheckForNull ZipCryptoParameters getCryptoParameters();

    /**
     * Returns the total number of (compressed) bytes this stream has written
     * to the underlying stream.
     */
    public long length() {
        return this.dos.size();
    }

    /**
     * Returns {@code true} if and only if this
     * {@code RawZipOutputStream} is currently writing a ZIP entry.
     */
    public boolean isBusy() {
        return null != this.entry;
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
     * (and hence require space), but only the last will be listed in the
     * central directory.
     * This is unlike the genuine {@link java.util.zip.ZipOutputStream
     * java.util.zip.ZipOutputStream} which would throw a {@link ZipException}
     * in this method when another entry with the same name is to be written.
     *
     * @param  entry The entry to write.
     * @param  process Whether or not the entry contents should get processed,
     *         e.g. deflated.
     *         This should be set to {@code false} if and only if the
     *         application is going to copy entries from an input ZIP file to
     *         an output ZIP file.
     *         The entries' CRC-32, compressed size and uncompressed
     *         size properties must be set in advance.
     * @throws ZipException If and only if writing the entry is impossible
     *         because the resulting file would not comply to the ZIP file
     *         format specification.
     * @throws IOException On any I/O error.
     */
    @SuppressWarnings("unchecked")
    public void putNextEntry(final E entry, final boolean process)
    throws IOException {
        closeEntry();
        final OutputMethod method = newOutputMethod(entry, process);
        final OutputStream out = method.init(entry);
        this.entry = entry;
        method.start();
        this.processor = method;
        this.delegate = out;
        // Store entry now so that a subsequent call to getEntry(...) returns
        // it.
        entries.put(entry.getName(), entry);
    }

    /**
     * Returns a new output method for the given entry.
     * Except the property &quot;method&quot;, this method must not modify the
     * given entry.
     */
    @SuppressWarnings("unchecked")
    private OutputMethod newOutputMethod(
            final ZipEntry entry,
            final boolean process)
    throws ZipException {
        // Order is important here!
        OutputMethod processor = new RawOutputMethod(process);
        if (!process) {
            assert UNKNOWN != entry.getCrc();
            return processor;
        }
        if (entry.isEncrypted())
            processor = newEncryptedOutputMethod(
                    (RawOutputMethod) processor,
                    getCryptoParameters());
        int method = entry.getMethod();
        if (UNKNOWN == method)
            entry.setMethod16(method = getMethod());
        switch (method) {
            case STORED:
                processor = new StoredOutputMethod(processor);
                break;
            case DEFLATED:
                processor = new DeflatedOutputMethod(processor);
                break;
            default:
                throw new ZipException(entry.getName()
                        + " (unsupported compression method "
                        + method
                        + ")");
        }
        return processor;
    }

    /**
     * Returns a new {@code EncryptedOutputMethod}.
     *
     * @param  processor the output method to decorate.
     * @param  param the {@link ZipCryptoParameters} used to determine and
     *         configure the type of the encrypted ZIP file.
     *         If the run time class of this parameter matches multiple
     *         parameter interfaces, it is at the discretion of this
     *         implementation which one is picked and hence which type of
     *         encrypted ZIP file is created.
     *         If you need more control over this, pass in an instance which's
     *         run time class just implements the
     *         {@link ZipCryptoParametersProvider} interface.
     *         Instances of this interface are queried to find crypto
     *         parameters which match a known encrypted ZIP file type.
     *         This algorithm is recursively applied.
     * @return A new {@code EncryptedOutputMethod}.
     * @throws ZipCryptoParametersException if {@code param} is {@code null} or
     *         no suitable crypto parameters can get found.
     * @throws IOException on any I/O error.
     */
    private EncryptedOutputMethod newEncryptedOutputMethod(
            final RawOutputMethod processor,
            final @CheckForNull ZipCryptoParameters param)
    throws ZipCryptoParametersException {
        assert null != processor;
        // Order is important here to support multiple interface implementations!
        if (param == null) {
            throw new ZipCryptoParametersException("No crypto parameters available!");
        } else if (param instanceof WinZipAesParameters) {
            return new WinZipAesOutputMethod(processor,
                    (WinZipAesParameters) param);
        } else if (param instanceof ZipCryptoParametersProvider) {
            return newEncryptedOutputMethod(processor,
                    ((ZipCryptoParametersProvider) param).get(
                        ZipCryptoParameters.class));
        } else {
            throw new ZipCryptoParametersException();
        }
    }

    private static void checkLocalFileHeaderProperties(final ZipEntry entry)
    throws ZipException {
        if (UNKNOWN == entry.getCrc())
            throw new ZipException(entry.getName() + " (unknown CRC checksum)");
        if (UNKNOWN == entry.getCompressedSize32())
            throw new ZipException(entry.getName() + " (unknown compressed size)");
        if (UNKNOWN == entry.getSize32())
            throw new ZipException(entry.getName() + " (unknown uncompressed size)");
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
        try {
            this.processor.finish();
        } finally {
            this.delegate = this.dos;
            this.processor = null;
            this.entry = null;
        }
        flush();
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
        final LEDataOutputStream dos = this.dos;
        this.cdOffset = dos.size();
        for (ZipEntry entry : this.entries.values())
            writeCentralFileHeader(entry);
        writeEndOfCentralDirectory();
    }

    /**
     * Writes a Central File Header record.
     *
     * @throws IOException On any I/O error.
     */
    private void writeCentralFileHeader(final ZipEntry entry)
    throws IOException {
        assert null != entry;
        final LEDataOutputStream dos = this.dos;
        final long csize32 = entry.getCompressedSize32();
        final long size32 = entry.getSize32();
        final long offset32 = entry.getOffset32();
        final boolean zip64 // ZIP64 extensions?
                =  csize32  >= UInt.MAX_VALUE
                || size32   >= UInt.MAX_VALUE
                || offset32 >= UInt.MAX_VALUE
                || FORCE_ZIP64_EXT;
        final int method = entry.getMethod();
        final boolean directory = entry.isDirectory();
        final int version = zip64
                ? 45
                : DEFLATED == method || directory
                    ? 20
                    : 10;
        // Central File Header.
        dos.writeInt(CFH_SIG);
        // Version Made By.
        dos.writeShort((entry.getPlatform() << 8) | 63);
        // Version Needed To Extract.
        dos.writeShort(version);
        // General Purpose Bit Flags.
        dos.writeShort(entry.getGeneral16());
        // Compression Method.
        dos.writeShort(method);
        // Last Mod. File Time / Date.
        dos.writeInt((int) entry.getTimeDos());
        // CRC-32.
        dos.writeInt((int) entry.getCrc());
        // Compressed Size.
        dos.writeInt((int) csize32);
        // Uncompressed Size.
        dos.writeInt((int) size32);
        // File Name Length.
        final byte[] name = encode(entry.getName());
        dos.writeShort(name.length);
        // Extra Field Length.
        final byte[] extra = entry.getExtra();
        assert extra != null;
        dos.writeShort(extra.length);
        // File Comment Length.
        final byte[] comment = getEntryComment(entry);
        dos.writeShort(comment.length);
        // Disk Number Start.
        dos.writeShort(0);
        // Internal File Attributes.
        dos.writeShort(0);
        // External File Attributes.
        dos.writeInt(directory ? 0x10 : 0); // fixed issue #27.
        // Relative Offset Of Local File Header.
        dos.writeInt((int) offset32);
        // File Name.
        dos.write(name);
        // Extra Field(s).
        dos.write(extra);
        // File Comment.
        dos.write(comment);
    }

    private byte[] getEntryComment(final ZipEntry entry) {
        return encode(entry.getEffectiveComment());
    }

    /**
     * Writes the End Of Central Directory record.
     *
     * @throws IOException On any I/O error.
     */
    private void writeEndOfCentralDirectory() throws IOException {
        final LEDataOutputStream dos = this.dos;
        final long cdEntries = entries.size();
        final long cdSize = dos.size() - cdOffset;
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
                    = dos.size();
            // ZIP64 End Of Central Directory Record signature.
            dos.writeInt(ZIP64_EOCDR_SIG);
            // Size Of ZIP64 End Of Central Directory Record.
            dos.writeLong(ZIP64_EOCDR_MIN_LEN - 12);
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
            // ZIP64 End Of Central Directory Locator signature.
            dos.writeInt(ZIP64_EOCDL_SIG);
            // Number Of The Disk With The Start Of The ZIP64 End Of Central Directory.
            dos.writeInt(0);
            // Relative Offset Of The ZIP64 End Of Central Directory record.
            dos.writeLong(zip64eocdOffset);
            // Total Number Of Disks.
            dos.writeInt(1);
        }
        // End Of Central Directory record signature.
        dos.writeInt(EOCDR_SIG);
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
        final byte[] comment = getRawComment();
        dos.writeShort(comment.length);
        dos.write(comment);
    }

    private byte[] getRawComment() {
        final byte[] comment = this.comment;
        return null != comment ? comment : EMPTY;
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

    /* Adjusts the number of written bytes for appending mode. */
    private static final class AppendingLEDataOutputStream
    extends LEDataOutputStream {
        AppendingLEDataOutputStream(OutputStream out, RawZipFile<?> appendee) {
            super(out);
            super.written = appendee.getOffsetMapper().location(appendee.length());
        }
    } // AppendingLEDataOutputStream

    private final class RawOutputMethod implements OutputMethod {

        final boolean process;

        RawOutputMethod(final boolean process) {
            this.process = process;
        }

        @Override
        public LEDataOutputStream init(final ZipEntry entry)
        throws ZipException {
            {
                final long size = encode(entry.getName()).length
                                + entry.getExtraLength()
                                + encode(entry.getEffectiveComment()).length;
                if (UShort.MAX_VALUE < size)
                    throw new ZipException(entry.getName()
                    + " (the total size " + size + " for the name, extra fields and comment is too long)");
            }
            int method = entry.getMethod();
            switch (method) {
                case STORED:
                    checkLocalFileHeaderProperties(entry);
                    break;
                case DEFLATED:
                    if (!this.process)
                        checkLocalFileHeaderProperties(entry);
                    break;
                default:
                    throw new AssertionError();
            }
            if (UNKNOWN == entry.getPlatform())
                entry.setPlatform8(PLATFORM_FAT);
            if (UNKNOWN == entry.getTime())
                entry.setTime(System.currentTimeMillis());
            return RawZipOutputStream.this.dos;
        }

        /**
         * Writes the Local File Header.
         */
        @Override
        public void start() throws IOException {
            final LEDataOutputStream dos = RawZipOutputStream.this.dos;
            final ZipEntry entry = RawZipOutputStream.this.entry;
            final long crc = entry.getCrc();
            final long csize = entry.getCompressedSize();
            final long size = entry.getSize();
            final long csize32 = entry.getCompressedSize32();
            final long size32 = entry.getSize32();
            final long offset = dos.size();
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
            final int method = entry.getMethod();
            final boolean directory = entry.isDirectory();
            final int version = zip64
                    ? 45
                    : DEFLATED == method || directory
                        ? 20
                        : 10;
            // Compose General Purpose Bit Flag.
            // See appendix D of PKWARE's ZIP File Format Specification.
            final boolean utf8 = UTF8.equals(charset);
            final int general = (encrypted ? (1 << GPBF_ENCRYPTED) : 0)
                              | (dd        ? (1 << GPBF_DATA_DESCRIPTOR) : 0)
                              | (utf8      ? (1 << GPBF_UTF8) : 0);
            // Start changes.
            RawZipOutputStream.this.finished = false;
            // Local File Header Signature.
            dos.writeInt(LFH_SIG);
            // Version Needed To Extract.
            dos.writeShort(version);
            // General Purpose Bit Flag.
            dos.writeShort(general);
            // Compression Method.
            dos.writeShort(entry.getMethod());
            // Last Mod. Time / Date in DOS format.
            dos.writeInt((int) entry.getTimeDos());
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
            final byte[] name = encode(entry.getName());
            dos.writeShort(name.length);
            // Extra Field Length.
            final byte[] extra = entry.getExtra(!dd);
            dos.writeShort(extra.length);
            // File Name.
            dos.write(name);
            // Extra Field(s).
            dos.write(extra);
            // Commit changes.
            entry.setGeneral16(general);
            entry.setOffset64(offset);
            // Update data start.
            RawZipOutputStream.this.dataStart = dos.size();
        }

        /**
         * Writes the Data Descriptor.
         */
        @Override
        public void finish() throws IOException {
            final LEDataOutputStream dos = RawZipOutputStream.this.dos;
            final long csize = RawZipOutputStream.this.dos.size()
                    - RawZipOutputStream.this.dataStart;
            final ZipEntry entry = RawZipOutputStream.this.entry;
            if (entry.getCompressedSize() != csize) {
                throw new ZipException(entry.getName()
                + " (bad compressed entry size "
                + entry.getCompressedSize()
                + ", expected "
                + csize
                + ")");
            }
            if (!entry.getGeneral1(GPBF_DATA_DESCRIPTOR))
                return;
            final long crc = entry.getCrc();
            final long size = entry.getSize();
            final long offset = entry.getOffset();
            // Offset MUST be considered in decision about ZIP64 format - see
            // description of Data Descriptor in ZIP File Format Specification!
            final boolean zip64 // ZIP64 extensions?
                    =  UInt.MAX_VALUE <= csize
                    || UInt.MAX_VALUE <= size
                    || UInt.MAX_VALUE <= offset
                    || FORCE_ZIP64_EXT;
            // Data Descriptor Signature.
            dos.writeInt(DD_SIG);
            // CRC-32.
            dos.writeInt(UNKNOWN != crc ? (int) crc : 0);
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
    } // RawOutputMethod

    private final class DeflatedOutputMethod
    extends DecoratingOutputMethod<OutputMethod> {

        @CheckForNull UpdatingCrc32OutputStream ucos;
        @CheckForNull ZipDeflaterOutputStream zdos;

        DeflatedOutputMethod(OutputMethod processor) {
            super(processor);
        }

        @Override
        public OutputStream init(ZipEntry entry) throws IOException {
            assert null == this.zdos;
            assert null == this.ucos;
            return this.ucos = new UpdatingCrc32OutputStream(
                    this.zdos = new ZipDeflaterOutputStream(
                        this.delegate.init(entry)));
        }

        @Override
        public void finish() throws IOException {
            // It seems tempting to apply the decorator pattern to the finish()
            // methods of the output streams.
            // However, the output streams would then finish ALL decorated
            // output streams recursively too, which is NOT what we want.
            // To inhibit this, we could introduce another decorated output
            // stream with a no-op finish() method, but then this output stream
            // would be unnecessarily processed in ALL output operations too,
            // which would only increase overhead.
            assert null != this.zdos;
            assert null != this.ucos;
            this.ucos.finish();
            this.zdos.finish();
            this.delegate.finish();
        }
    } // DeflatedOutputMethod

    private final class StoredOutputMethod
    extends DecoratingOutputMethod<OutputMethod> {

        @CheckForNull CheckingCrc32OutputStream out;

        StoredOutputMethod(OutputMethod processor) {
            super(processor);
        }

        @Override
        public OutputStream init(ZipEntry entry) throws IOException {
            assert null == this.out;
            // The CRC-32 value must get backed up because it may get modified
            // by the output method decorator chain as a side effect, e.g. when
            // writing WinZip AES entries with AE-2 as the vendor version.
            final long crc = entry.getCrc();
            return this.out = new CheckingCrc32OutputStream(
                    delegate.init(entry), crc);
        }

        @Override
        public void finish() throws IOException {
            // see DeflatedOutputMethod.finish().
            assert null != this.out;
            this.out.finish();
            this.delegate.finish();
        }
    } // StoredOutputMethod

    private abstract class EncryptedOutputMethod
    extends DecoratingOutputMethod<RawOutputMethod> {

        EncryptedOutputMethod(RawOutputMethod processor) {
            super(processor);
        }
    } // EncryptedOutputMethod

    private final class WinZipAesOutputMethod
    extends EncryptedOutputMethod {

        final WinZipAesParameters param;
        @CheckForNull WinZipAesEntryOutputStream out;

        WinZipAesOutputMethod(
                RawOutputMethod processor,
                final WinZipAesParameters param)
        throws ZipKeyException {
            super(processor);
            assert null != param;
            this.param = param;
        }

        @Override
        public OutputStream init(final ZipEntry entry) throws IOException {
            assert null == this.out;
            // Order is critical here!
            final WinZipAesEntryParameters
                    param = new WinZipAesEntryParameters(this.param, entry);
            final AesKeyStrength keyStrength = param.getKeyStrength();
            final LEDataOutputStream dos = delegate.init(entry);
            final WinZipAesExtraField field = new WinZipAesExtraField();
            field.setKeyStrength(keyStrength);
            final int method = entry.getMethod();
            field.setMethod(method);
            final long size = entry.getSize();
            field.setVendorVersion(size >= 20 /* && BZIP2 != method */ ? VV_AE_1 : VV_AE_2);
            entry.setMethod16(WINZIP_AES);
            entry.addExtraField(field);
            return this.out = new WinZipAesEntryOutputStream(dos, param);
        }

        @Override
        public void start() throws IOException {
            // see DeflatedOutputMethod.finish().
            assert null != this.out;
            this.delegate.start();
            this.out.start();
        }

        @Override
        public void finish() throws IOException {
            // see DeflatedOutputMethod.finish().
            assert null != this.out;
            this.out.finish();
            this.delegate.finish();
        }
    } // WinZipAesOutputMethod

    private final class ZipDeflaterOutputStream
    extends DeflaterOutputStream {

        ZipDeflaterOutputStream(OutputStream out) {
            super(out, RawZipOutputStream.this.deflater, MAX_FLATER_BUF_LENGTH);
        }

        @Override
        public void finish() throws IOException {
            super.finish();
            final E entry = RawZipOutputStream.this.entry;
            final ZipDeflater deflater = RawZipOutputStream.this.deflater;
            entry.setCompressedSize64(deflater.getBytesWritten());
            entry.setSize64(deflater.getBytesRead());
            deflater.reset();
        }
    } // ZipDeflaterOutputStream

    private final class UpdatingCrc32OutputStream
    extends Crc32OutputStream {

        UpdatingCrc32OutputStream(OutputStream out) {
            super(out);
        }

        void finish() {
            final E entry = RawZipOutputStream.this.entry;
            entry.setCrc32(getChecksum().getValue());
        }
    } // UpdatingCrc32OutputStream

    private final class CheckingCrc32OutputStream
    extends Crc32OutputStream {

        final long crc;

        CheckingCrc32OutputStream(OutputStream out, long crc) {
            super(out);
            this.crc = crc;
        }

        void finish() throws ZipException {
            final E entry = RawZipOutputStream.this.entry;
            final long crc = getChecksum().getValue();
            if (this.crc != crc)
                throw new CRC32Exception(entry.getName(), this.crc, crc);
        }
    } // CheckingCrc32OutputStream
}
