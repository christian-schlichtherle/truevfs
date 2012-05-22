/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.zip.io;

import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.ZipException;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.WillNotClose;
import javax.annotation.concurrent.NotThreadSafe;
import static net.truevfs.driver.zip.io.Constants.*;
import static net.truevfs.driver.zip.io.ExtraField.WINZIP_AES_ID;
import static net.truevfs.driver.zip.io.WinZipAesEntryExtraField.VV_AE_1;
import static net.truevfs.driver.zip.io.WinZipAesEntryExtraField.VV_AE_2;
import static net.truevfs.driver.zip.io.WinZipAesUtils.overhead;
import static net.truevfs.driver.zip.io.ZipEntry.*;
import static net.truevfs.driver.zip.io.ZipParametersUtils.parameters;
import net.truevfs.kernel.io.DecoratingOutputStream;
import net.truevfs.kernel.io.LittleEndianOutputStream;
import net.truevfs.kernel.io.Sink;
import static net.truevfs.kernel.util.HashMaps.initialCapacity;
import net.truevfs.key.param.AesKeyStrength;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;

/**
 * Provides unsafe (raw) access to a ZIP file using shared {@link ZipEntry}
 * instances.
 * <p>
 * <b>Warning:</b> This class is <em>not</em> intended for public use
 * - its API may change at will without prior notification!
 *
 * @param  <E> the type of the ZIP entries.
 * @see    RawZipFile
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public abstract class RawZipOutputStream<E extends ZipEntry>
extends DecoratingOutputStream
implements Iterable<E> {

    private final LittleEndianOutputStream leos;

    /** The charset to use for entry names and comments. */
    private final Charset charset;

    /** Default compression method for next entry. */
    private int method;

    /** Default compression level for the methods DEFLATED and BZIP2. */
    private int level;

    /** The encoded file comment. */
    private @CheckForNull byte[] comment;

    /**
     * The list of ZIP entries started to be written so far.
     * Maps entry names to zip entries.
     */
    private final Map<String, E> entries;

    /** Start of central directory. */
    private long cdOffset;

    private boolean finished;

    /** Current ZIP entry. */
    private @Nullable ZipEntry entry;

    private @Nullable OutputMethod processor;

    /**
     * Constructs a raw ZIP output stream which decorates the given output
     * stream and optionally apppends to the given raw ZIP file.
     *
     * @param  sink the sink to write the ZIP file to.
     *         If {@code appendee} is not {@code null}, then this must be set
     *         up so that it appends to the same ZIP file from which
     *         {@code appendee} is reading.
     * @param  appendee the nullable raw ZIP file to append to.
     *         This may already be closed.
     * @param  param the parameters for writing the ZIP file.
     */
    @CreatesObligation
    protected RawZipOutputStream(
            final Sink sink,
            final @CheckForNull @WillNotClose RawZipFile<E> appendee,
            final ZipOutputStreamParameters param)
    throws IOException {
        final OutputStream out = sink.stream();
        try {
            this.out = this.leos = null != appendee
                    ? new AppendingLittleEndianOutputStream(out, appendee)
                    : new LittleEndianOutputStream(out);
            if (null != appendee) {
                this.charset = appendee.getRawCharset();
                this.comment = appendee.getRawComment();
                final Map<String, E> entries = new LinkedHashMap<>(
                        initialCapacity(appendee.size() + param.getOverheadSize()));
                entries.putAll(appendee.getRawEntries());
                this.entries = entries;
            } else {
                this.charset = param.getCharset();
                this.entries = new LinkedHashMap<>(
                        initialCapacity(param.getOverheadSize()));
            }
            setMethod0(param.getMethod());
            setLevel0(param.getLevel());
        } catch (final Throwable ex) {
            try {
                out.close();
            } catch (final Throwable ex2) {
                ex.addSuppressed(ex2);
            }
            throw ex;
        }
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
     * Returns an iteration of all entries written to this ZIP file so
     * far.
     * Note that the iterated entries are shared with this instance.
     * It is illegal to put more entries into this ZIP output stream
     * concurrently or modify the state of the iterated entries.
     */
    @Override
    public Iterator<E> iterator() {
        return Collections.unmodifiableCollection(entries.values()).iterator();
    }

    /**
     * Returns the entry for the given {@code name} or {@code null} if no entry
     * with this name exists in this ZIP file.
     * Note that the returned entry is shared with this instance - it is an
     * error to change its state!
     *
     * @param  name the name of the ZIP entry.
     * @return The entry for the given {@code name} or {@code null} if no entry
     *         with this name exists in this ZIP file.
     */
    public E entry(String name) {
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
     * The initial value is {@link ZipEntry#DEFLATED}.
     *
     * @see #setMethod
     * @see ZipEntry#getMethod
     */
    public int getMethod() {
        return method;
    }

    /**
     * Sets the default compression method for entries.
     * This property is only used if a {@link ZipEntry} does not specify a
     * compression method.
     * Legal values are {@link ZipEntry#STORED}, {@link ZipEntry#DEFLATED}
     * and {@link ZipEntry#BZIP2}.
     *
     * @param  method the default compression method for entries.
     * @throws IllegalArgumentException if the method is invalid.
     * @see    #getMethod
     * @see    ZipEntry#setMethod
     */
    public void setMethod(final int method) {
        setMethod0(method);
    }

    private void setMethod0(final int method) {
        final ZipEntry test = new ZipEntry("");
        test.setMethod(method);
        this.method = test.getMethod();
    }

    /**
     * Returns the compression level for entries.
     * This property is only used if the effective compression method is
     * {@link ZipEntry#DEFLATED} or {@link ZipEntry#BZIP2}.
     * 
     * @return The compression level for entries.
     * @see    #setLevel
     */
    public int getLevel() {
        return level;
    }

    /**
     * Sets the compression level for entries.
     * This property is only used if the effective compression method is
     * {@link ZipEntry#DEFLATED} or {@link ZipEntry#BZIP2}.
     * Legal values are {@link Deflater#DEFAULT_COMPRESSION} or range from
     * {@code Deflater#BEST_SPEED} to {@code Deflater#BEST_COMPRESSION}.
     * 
     * @param  level the compression level for entries.
     * @throws IllegalArgumentException if the compression level is invalid.
     * @see    #getLevel
     */
    public void setLevel(int level) {
	setLevel0(level);
    }

    private void setLevel0(int level) {
        if ((level < Deflater.BEST_SPEED || Deflater.BEST_COMPRESSION < level)
                && Deflater.DEFAULT_COMPRESSION != level)
            throw new IllegalArgumentException("Invalid compression level!");
        this.level = level;
    }

    /**
     * Returns the parameters for encryption or authentication of entries.
     * 
     * Returns The parameters for encryption or authentication of entries.
     */
    protected abstract @CheckForNull ZipCryptoParameters getCryptoParameters();

    /**
     * Returns the total number of (compressed) bytes this stream has written
     * to the underlying stream.
     */
    public long length() {
        return leos.size();
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
    public void putNextEntry(final E entry, final boolean process)
    throws ZipException, IOException {
        closeEntry();
        final OutputMethod method = newOutputMethod(entry, process);
        method.init(entry.clone()); // test!
        method.init(entry);
        this.out = method.start();
        this.processor = method;
        // Store entry now so that a subsequent call to entry(...) returns
        // it.
        this.entries.put(entry.getName(), entry);
        this.entry = entry;
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
        // HC SVNT DRACONES!
        OutputMethod processor = new RawOutputMethod(process);
        if (!process) {
            assert UNKNOWN != entry.getCrc();
            return processor;
        }
        int method = entry.getMethod();
        if (UNKNOWN == method)
            entry.setRawMethod(method = getMethod());
        boolean skipCrc = false;
        if (entry.isEncrypted() || WINZIP_AES == method) {
            ZipCryptoParameters param = getCryptoParameters();
            if (WINZIP_AES == method) {
                param = parameters(WinZipAesParameters.class, param);
                final WinZipAesEntryExtraField field = (WinZipAesEntryExtraField)
                        entry.getExtraField(WINZIP_AES_ID);
                if (null != field) {
                    method = field.getMethod();
                    if (VV_AE_2 == field.getVendorVersion())
                        skipCrc = true;
                }
            }
            processor = newEncryptedOutputMethod((RawOutputMethod) processor,
                    param);
        }
        switch (method) {
            case STORED:
                if (!skipCrc)
                    processor = new Crc32CheckingOutputMethod(processor);
                break;
            case DEFLATED:
                processor = new DeflaterOutputMethod(processor);
                if (!skipCrc)
                    processor = new Crc32UpdatingOutputMethod(processor);
                break;
            case BZIP2:
                processor = new BZip2OutputMethod(processor);
                if (!skipCrc)
                    processor = new Crc32UpdatingOutputMethod(processor);
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
     *         {@link ZipParametersProvider} interface.
     *         Instances of this interface are queried to find crypto
     *         parameters which match a known encrypted ZIP file type.
     *         This algorithm is recursively applied.
     * @return A new {@code EncryptedOutputMethod}.
     * @throws ZipCryptoParametersException if {@code param} is {@code null} or
     *         no suitable crypto parameters can get found.
     */
    private EncryptedOutputMethod newEncryptedOutputMethod(
            final RawOutputMethod processor,
            @CheckForNull ZipParameters param)
    throws ZipParametersException {
        assert null != processor;
        while (null != param) {
            // Order is important here to support multiple interface implementations!
            if (param instanceof WinZipAesParameters) {
                return new WinZipAesOutputMethod(processor,
                        (WinZipAesParameters) param);
            } else if (param instanceof ZipParametersProvider) {
                param = ((ZipParametersProvider) param)
                        .get(ZipCryptoParameters.class);
            } else {
                break;
            }
        }
        throw new ZipParametersException("No suitable crypto parameters available!");
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
        final ZipEntry entry = this.entry;
        if (null == entry)
            return;
        this.processor.finish();
        this.out.flush();
        this.out = this.leos;
        this.processor = null;
        this.entry = null;
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
        closeEntry();
        final LittleEndianOutputStream leos = this.leos;
        this.cdOffset = leos.size();
        final Iterator<E> i = this.entries.values().iterator();
        while (i.hasNext())
            if (!writeCentralFileHeader(i.next()))
                i.remove();
        writeEndOfCentralDirectory();
        this.finished = true;
    }

    /**
     * Writes a Central File Header record.
     *
     * @return {@code false} if and only if the record has been skipped,
     *         i.e. not written for some other reason than an I/O error.
     * @throws IOException On any I/O error.
     */
    private boolean writeCentralFileHeader(final ZipEntry entry)
    throws IOException {
        final long csize = entry.getCompressedSize();
        final long size = entry.getSize();
        // This test MUST NOT include the CRC-32 because VV_AE_2 sets it to
        // UNKNOWN!
        if (UNKNOWN == (csize | size)) {
            // See http://java.net/jira/browse/TRUEZIP-144 :
            // The kernel may set any of these properties to UNKNOWN after the
            // entry content has already been written in order to signal that
            // this entry should not get included in the central directory.
            // E.g. this may happen with the GROW output option preference.
            return false;
        }
        final LittleEndianOutputStream leos = this.leos;
        // central file header signature   4 bytes  (0x02014b50)
        leos.writeInt(CFH_SIG);
        // version made by                 2 bytes
        leos.writeShort((entry.getRawPlatform() << 8) | 63);
        // version needed to extract       2 bytes
        leos.writeShort(entry.getRawVersionNeededToExtract());
        // general purpose bit flag        2 bytes
        leos.writeShort(entry.getGeneralPurposeBitFlags());
        // compression method              2 bytes
        leos.writeShort(entry.getRawMethod());
        // last mod file time              2 bytes
        // last mod file date              2 bytes
        leos.writeInt((int) entry.getRawTime());
        // crc-32                          4 bytes
        leos.writeInt((int) entry.getRawCrc());
        // compressed size                 4 bytes
        leos.writeInt((int) entry.getRawCompressedSize());
        // uncompressed size               4 bytes
        leos.writeInt((int) entry.getRawSize());
        // file name length                2 bytes
        final byte[] name = encode(entry.getName());
        leos.writeShort(name.length);
        // extra field length              2 bytes
        final byte[] extra = entry.getRawExtraFields();
        leos.writeShort(extra.length);
        // file comment length             2 bytes
        final byte[] comment = getCommentEncoded(entry);
        leos.writeShort(comment.length);
        // disk number start               2 bytes
        leos.writeShort(0);
        // internal file attributes        2 bytes
        leos.writeShort(0);
        // external file attributes        4 bytes
        leos.writeInt((int) entry.getRawExternalAttributes());
        // relative offset of local header 4 bytes
        leos.writeInt((int) entry.getRawOffset());
        // file name (variable size)
        leos.write(name);
        // extra field (variable size)
        leos.write(extra);
        // file comment (variable size)
        leos.write(comment);

        return true;
    }

    private byte[] getCommentEncoded(final ZipEntry entry) {
        return encode(entry.getRawComment());
    }

    /**
     * Writes the End Of Central Directory record.
     *
     * @throws IOException On any I/O error.
     */
    private void writeEndOfCentralDirectory() throws IOException {
        final LittleEndianOutputStream leos = this.leos;
        final long cdEntries = entries.size();
        final long cdOffset = this.cdOffset;
        final long cdSize = leos.size() - cdOffset;
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
                    = leos.size();
            // zip64 end of central dir 
            // signature                       4 bytes  (0x06064b50)
            leos.writeInt(ZIP64_EOCDR_SIG);
            // size of zip64 end of central
            // directory record                8 bytes
            leos.writeLong(ZIP64_EOCDR_MIN_LEN - 12);
            // version made by                 2 bytes
            leos.writeShort(63);
            // version needed to extract       2 bytes
            leos.writeShort(46); // due to potential use of BZIP2 compression
            // number of this disk             4 bytes
            leos.writeInt(0);
            // number of the disk with the 
            // start of the central directory  4 bytes
            leos.writeInt(0);
            // total number of entries in the
            // central directory on this disk  8 bytes
            leos.writeLong(cdEntries);
            // total number of entries in the
            // central directory               8 bytes
            leos.writeLong(cdEntries);
            // size of the central directory   8 bytes
            leos.writeLong(cdSize);
            // offset of start of central
            // directory with respect to
            // the starting disk number        8 bytes
            leos.writeLong(cdOffset);
            // zip64 extensible data sector    (variable size)
            //
            // zip64 end of central dir locator 
            // signature                       4 bytes  (0x07064b50)
            leos.writeInt(ZIP64_EOCDL_SIG);
            // number of the disk with the
            // start of the zip64 end of 
            // central directory               4 bytes
            leos.writeInt(0);
            // relative offset of the zip64
            // end of central directory record 8 bytes
            leos.writeLong(zip64eocdOffset);
            // total number of disks           4 bytes
            leos.writeInt(1);
        }
        // end of central dir signature    4 bytes  (0x06054b50)
        leos.writeInt(EOCDR_SIG);
        // number of this disk             2 bytes
        leos.writeShort(0);
        // number of the disk with the
        // start of the central directory  2 bytes
        leos.writeShort(0);
        // total number of entries in the
        // central directory on this disk  2 bytes
        leos.writeShort(cdEntries16);
        // total number of entries in
        // the central directory           2 bytes
        leos.writeShort(cdEntries16);
        // size of the central directory   4 bytes
        leos.writeInt((int) cdSize32);
        // offset of start of central
        // directory with respect to
        // the starting disk number        4 bytes
        leos.writeInt((int) cdOffset32);
        // .ZIP file comment length        2 bytes
        final byte[] comment = getRawComment();
        leos.writeShort(comment.length);
        // .ZIP file comment       (variable size)
        leos.write(comment);
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
        finish();
        out.close();
    }

    /** Adjusts the number of written bytes in the offset for appending mode. */
    private static final class AppendingLittleEndianOutputStream
    extends LittleEndianOutputStream {
        AppendingLittleEndianOutputStream(
                final @WillCloseWhenClosed OutputStream out,
                final @WillNotClose RawZipFile<?> appendee) {
            super(out);
            super.written = appendee.getOffsetMapper().unmap(appendee.length());
        }
    } // AppendingLEDataOutputStream

    private final class RawOutputMethod implements OutputMethod {
        final boolean process;

        /** Start of entry data. */
        private long dataStart;
        @Nullable ZipEntry entry;

        RawOutputMethod(final boolean process) {
            this.process = process;
        }

        @Override
        public void init(final ZipEntry entry) throws ZipException {
            {
                final long size = encode(entry.getName()).length
                                + entry.getRawExtraFields().length
                                + encode(entry.getRawComment()).length;
                if (UShort.MAX_VALUE < size)
                    throw new ZipException(entry.getName()
                            + " (the total size of "
                            + size
                            + " bytes for the name, extra fields and comment exceeds the maximum size of "
                            + UShort.MAX_VALUE + " bytes)");
            }
            if (STORED == entry.getMethod() || !this.process) {
                if (UNKNOWN == entry.getCrc())
                    throw new ZipException(entry.getName()
                            + " (unknown CRC-32 value)");
                if (UNKNOWN == entry.getCompressedSize())
                    throw new ZipException(entry.getName()
                            + " (unknown compressed size)");
                if (UNKNOWN == entry.getSize())
                    throw new ZipException(entry.getName()
                            + " (unknown uncompressed size)");
            }
            if (UNKNOWN == entry.getPlatform())
                entry.setRawPlatform(PLATFORM_FAT);
            if (UNKNOWN == entry.getTime())
                entry.setTime(System.currentTimeMillis());
            this.entry = entry;
        }

        /**
         * Writes the Local File Header.
         */
        @Override
        public OutputStream start() throws IOException {
            final LittleEndianOutputStream leos = RawZipOutputStream.this.leos;
            final long offset = leos.size();
            final ZipEntry entry = this.entry;
            final boolean encrypted = entry.isEncrypted();
            final boolean dd = entry.isDataDescriptorRequired();
            // Compose General Purpose Bit Flag.
            // See appendix D of PKWARE's ZIP File Format Specification.
            final boolean utf8 = UTF8.equals(charset);
            final int general = (encrypted ? GPBF_ENCRYPTED : 0)
                              | (dd        ? GPBF_DATA_DESCRIPTOR : 0)
                              | (utf8      ? GPBF_UTF8 : 0);
            // Start changes.
            RawZipOutputStream.this.finished = false;
            // local file header signature     4 bytes  (0x04034b50)
            leos.writeInt(LFH_SIG);
            // version needed to extract       2 bytes
            leos.writeShort(entry.getRawVersionNeededToExtract());
            // general purpose bit flag        2 bytes
            leos.writeShort(general);
            // compression method              2 bytes
            leos.writeShort(entry.getRawMethod());
            // last mod file time              2 bytes
            // last mod file date              2 bytes
            leos.writeInt((int) entry.getRawTime());
            // crc-32                          4 bytes
            // compressed size                 4 bytes
            // uncompressed size               4 bytes
            if (dd) {
                leos.writeInt(0);
                leos.writeInt(0);
                leos.writeInt(0);
            } else {
                leos.writeInt((int) entry.getRawCrc());
                leos.writeInt((int) entry.getRawCompressedSize());
                leos.writeInt((int) entry.getRawSize());
            }
            // file name length                2 bytes
            final byte[] name = encode(entry.getName());
            leos.writeShort(name.length);
            // extra field length              2 bytes
            final byte[] extra = entry.getRawExtraFields();
            leos.writeShort(extra.length);
            // file name (variable size)
            leos.write(name);
            // extra field (variable size)
            leos.write(extra);
            // Commit changes.
            entry.setGeneralPurposeBitFlags(general);
            entry.setRawOffset(offset);
            // Update data start.
            this.dataStart = leos.size();
            return leos;
        }

        /**
         * Checks the compressed entry size and optionally writes the Data
         * Descriptor.
         */
        @Override
        public void finish() throws IOException {
            final LittleEndianOutputStream leos = RawZipOutputStream.this.leos;
            final long csize = leos.size() - this.dataStart;
            final ZipEntry entry = this.entry;
            assert UNKNOWN != entry.getCrc();
            assert UNKNOWN != entry.getSize();
            if (entry.getGeneralPurposeBitFlag(GPBF_DATA_DESCRIPTOR)) {
                entry.setRawCompressedSize(csize);
                // data descriptor signature       4 bytes  (0x08074b50)
                leos.writeInt(DD_SIG);
                // crc-32                          4 bytes
                leos.writeInt((int) entry.getRawCrc());
                // compressed size                 4 or 8 bytes
                // uncompressed size               4 or 8 bytes
                if (entry.isZip64ExtensionsRequired()) {
                    leos.writeLong(csize);
                    leos.writeLong(entry.getSize());
                } else {
                    leos.writeInt((int) entry.getRawCompressedSize());
                    leos.writeInt((int) entry.getRawSize());
                }
            } else if (entry.getCompressedSize() != csize) {
                throw new ZipException(entry.getName()
                        + " (expected compressed entry size of "
                        + entry.getCompressedSize()
                        + " bytes, but is actually "
                        + csize
                        + " bytes)");
            }
        }
    } // RawOutputMethod

    private abstract class EncryptedOutputMethod extends DecoratingOutputMethod {
        EncryptedOutputMethod(RawOutputMethod processor) {
            super(processor);
        }
    } // EncryptedOutputMethod

    private final class WinZipAesOutputMethod extends EncryptedOutputMethod {
        final WinZipAesParameters generalParam;
        boolean suppressCrc;
        @Nullable WinZipAesEntryParameters entryParam;
        @Nullable WinZipAesEntryOutputStream out;
        @Nullable ZipEntry entry;

        WinZipAesOutputMethod(
                RawOutputMethod processor,
                final WinZipAesParameters param) {
            super(processor);
            assert null != param;
            this.generalParam = param;
        }

        @Override
        public void init(final ZipEntry entry) throws ZipException {
            // HC SVNT DRACONES!
            final WinZipAesEntryParameters entryParam
                    = new WinZipAesEntryParameters(this.generalParam, entry);
            final AesKeyStrength keyStrength = entryParam.getKeyStrength();
            this.entryParam = entryParam;
            WinZipAesEntryExtraField field = null;
            int method = entry.getMethod();
            long csize = entry.getCompressedSize();
            if (WINZIP_AES == method) {
                field = (WinZipAesEntryExtraField) entry.getExtraField(
                        WINZIP_AES_ID);
                if (null != field) {
                    method = field.getMethod();
                    if (UNKNOWN != csize)
                        csize -= overhead(field.getKeyStrength());
                    entry.setRawMethod(method); // restore for out.init(*)
                }
            }
            if (null == field)
                field = new WinZipAesEntryExtraField();
            field.setKeyStrength(keyStrength);
            field.setMethod(method);
            final long size = entry.getSize();
            if (20 <= size && BZIP2 != method) {
                field.setVendorVersion(VV_AE_1);
            } else {
                field.setVendorVersion(VV_AE_2);
                this.suppressCrc = true;
            }
            entry.addExtraField(field);
            if (UNKNOWN != csize) {
                csize += overhead(keyStrength);
                entry.setRawCompressedSize(csize);
            }
            if (this.suppressCrc) {
                final long crc = entry.getCrc();
                entry.setRawCrc(0);
                this.method.init(entry);
                entry.setCrc(crc);
            } else {
                this.method.init(entry);
            }
            entry.setRawMethod(WINZIP_AES);
            this.entry = entry;
        }

        @Override
        public OutputStream start() throws IOException {
            // see DeflatedOutputMethod.finish().
            final ZipEntry entry = this.entry;
            final OutputMethod method = this.method;
            final WinZipAesEntryParameters entryParam = this.entryParam;
            assert null != entryParam;
            assert null == this.out;
            if (suppressCrc) {
                final long crc = entry.getCrc();
                entry.setRawCrc(0);
                this.out = new WinZipAesEntryOutputStream(entryParam,
                        (LittleEndianOutputStream) method.start());
                entry.setCrc(crc);
            } else {
                this.out = new WinZipAesEntryOutputStream(entryParam,
                        (LittleEndianOutputStream) method.start());
            }
            return this.out;
        }

        @Override
        public void finish() throws IOException {
            // see DeflatedOutputMethod.finish().
            assert null != this.out;
            this.out.finish();
            if (this.suppressCrc) {
                final ZipEntry entry = this.entry;
                entry.setRawCrc(0);
                this.method.finish();
                // Set to UNKNOWN in order to signal to
                // Crc32CheckingOutputMethod that it should not check it and
                // signal to writeCentralFileHeader() that it should write 0.
                entry.setCrc(UNKNOWN);
            } else {
                this.method.finish();
            }
        }
    } // WinZipAesOutputMethod

    private final class BZip2OutputMethod extends DecoratingOutputMethod {
        @Nullable BZip2CompressorOutputStream cout;
        @Nullable LittleEndianOutputStream dout;
        @Nullable ZipEntry entry;

        BZip2OutputMethod(OutputMethod processor) {
            super(processor);
        }

        @Override
        public void init(final ZipEntry entry) throws ZipException  {
            entry.setCompressedSize(UNKNOWN);
            this.method.init(entry);
            this.entry = entry;
        }

        @Override
        public OutputStream start() throws IOException {
            assert null == this.cout;
            assert null == this.dout;
            OutputStream out = this.method.start();
            final long size = this.entry.getSize();
            final int blockSize = UNKNOWN != size
                    ? BZip2CompressorOutputStream.chooseBlockSize(size)
                    : getBZip2BlockSize();
            out = this.cout = new BZip2CompressorOutputStream(out, blockSize);
            return this.dout = new LittleEndianOutputStream(out);
        }

        int getBZip2BlockSize() {
            final int level = RawZipOutputStream.this.getLevel();
            if (BZip2CompressorOutputStream.MIN_BLOCKSIZE <= level
                    && level <= BZip2CompressorOutputStream.MAX_BLOCKSIZE)
                return level;
            return BZip2CompressorOutputStream.MAX_BLOCKSIZE;
        }

        @Override
        public void finish()
        throws IOException {
            this.dout.flush(); // superfluous - should not buffer
            this.cout.finish();
            this.entry.setRawSize(this.dout.size());
            this.method.finish();
        }
    } // BZip2OutputMethod

    private final class DeflaterOutputMethod extends DecoratingOutputMethod {
        @Nullable ZipDeflaterOutputStream out;
        @Nullable ZipEntry entry;

        DeflaterOutputMethod(OutputMethod processor) {
            super(processor);
        }

        @Override
        public void init(final ZipEntry entry) throws ZipException  {
            entry.setCompressedSize(UNKNOWN);
            this.method.init(entry);
            this.entry = entry;
        }

        @Override
        public OutputStream start() throws IOException {
            assert null == this.out;
            return this.out = new ZipDeflaterOutputStream(
                    this.method.start(),
                    RawZipOutputStream.this.getLevel(),
                    MAX_FLATER_BUF_LENGTH);
        }

        @Override
        public void finish() throws IOException {
            this.out.finish();
            final Deflater deflater = this.out.getDeflater();
            final ZipEntry entry = this.entry;
            //entry.setRawCompressedSize(deflater.getBytesWritten());
            entry.setRawSize(deflater.getBytesRead());
            deflater.end();
            this.method.finish();
        }
    } // DeflaterOutputMethod

    private abstract class Crc32OutputMethod extends DecoratingOutputMethod {
        @Nullable Crc32OutputStream out;

        Crc32OutputMethod(OutputMethod processor) {
            super(processor);
        }

        @Override
        public OutputStream start() throws IOException {
            assert null == this.out;
            return this.out = new Crc32OutputStream(this.method.start());
        }

        @Override
        public abstract void finish() throws IOException;
    } // Crc32OutputMethod

    private final class Crc32CheckingOutputMethod extends Crc32OutputMethod {
        Crc32CheckingOutputMethod(OutputMethod processor) {
            super(processor);
        }

        @Override
        public void finish() throws IOException {
            this.method.finish();
            final ZipEntry entry = RawZipOutputStream.this.entry;
            final long expectedCrc = entry.getCrc();
            if (UNKNOWN != expectedCrc) {
                final long actualCrc = this.out.getChecksum().getValue();
                if (expectedCrc != actualCrc)
                    throw new Crc32Exception(entry.getName(), expectedCrc, actualCrc);
            }
        }
    } // Crc32CheckingOutputMethod

    private final class Crc32UpdatingOutputMethod extends Crc32OutputMethod {
        Crc32UpdatingOutputMethod(OutputMethod processor) {
            super(processor);
        }

        @Override
        public void finish() throws IOException {
            final ZipEntry entry = RawZipOutputStream.this.entry;
            final long crc = this.out.getChecksum().getValue();
            entry.setRawCrc(crc);
            this.method.finish();
        }
    } // Crc32UpdatingOutputMethod
}
