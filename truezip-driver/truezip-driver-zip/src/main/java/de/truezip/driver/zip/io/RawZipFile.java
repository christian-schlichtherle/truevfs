/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.io;

import static de.truezip.driver.zip.io.Constants.*;
import static de.truezip.driver.zip.io.ExtraField.WINZIP_AES_ID;
import static de.truezip.driver.zip.io.WinZipAesEntryExtraField.VV_AE_2;
import static de.truezip.driver.zip.io.WinZipAesUtils.overhead;
import static de.truezip.driver.zip.io.ZipEntry.*;
import static de.truezip.driver.zip.io.ZipParametersUtils.parameters;
import de.truezip.kernel.io.*;
import static de.truezip.kernel.util.Maps.initialCapacity;
import edu.umd.cs.findbugs.annotations.CleanupObligation;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import edu.umd.cs.findbugs.annotations.DischargesObligation;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.util.*;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import java.util.zip.Inflater;
import java.util.zip.ZipException;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.WillNotClose;
import javax.annotation.concurrent.NotThreadSafe;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

/**
 * Provides unsafe (raw) access to a ZIP file using shared {@link ZipEntry}
 * instances.
 * <p>
 * <b>Warning:</b> This class is <em>not</em> intended for public use
 * - its API may change at will without prior notification!
 * <p>
 * Where the constructors of this class accept a {@code charset}
 * parameter, this is used to decode comments and entry names in the ZIP file.
 * However, if an entry has bit 11 set in its General Purpose Bit Flags,
 * then this parameter is ignored and "UTF-8" is used for this entry.
 * This is in accordance to Appendix D of PKWARE's
 * <a href="http://www.pkware.com/documents/casestudies/APPNOTE.TXT">ZIP File Format Specification</a>,
 * version 6.3.0 and later.
 * <p>
 * This class is able to skip a preamble like the one found in self extracting
 * archives.
 *
 * @param  <E> the type of the ZIP entries.
 * @see    RawZipOutputStream
 * @author Christian Schlichtherle
 */
@NotThreadSafe
@CleanupObligation
public abstract class RawZipFile<E extends ZipEntry>
implements Closeable, Iterable<E> {

    private static final int LFH_FILE_NAME_LENGTH_POS =
            /* Local File Header signature     */ 4 +
            /* Version Needed To Extract       */ 2 +
            /* General Purpose Bit Flags       */ 2 +
            /* Compression Method              */ 2 +
            /* Last Mod File Time              */ 2 +
            /* Last Mod File Date              */ 2 +
            /* CRC-32                          */ 4 +
            /* Compressed Size                 */ 4 +
            /* Uncompressed Size               */ 4;

    /**
     * The default character set used for entry names and comments in ZIP files.
     * This is {@code "UTF-8"} for compatibility with Sun's JDK implementation.
     * Note that you should use &quot;IBM437&quot; for ordinary ZIP files
     * instead.
     */
    public static final Charset DEFAULT_CHARSET = Constants.DEFAULT_CHARSET;

    /** The nullable seekable byte channel. */
    private @CheckForNull SeekableByteChannel channel;

    /** The total number of bytes in the ZIP channel. */
    private long length;

    /** The number of bytes in the preamble of this ZIP file. */
    private long preamble;

    /** The number of bytes in the postamble of this ZIP file. */
    private long postamble;

    private final ZipEntryFactory<E> param;

    /** The charset to use for entry names and comments. */
    private Charset charset;

    /** The encoded file comment. */
    private @CheckForNull byte[] comment;

    /** Maps entry names to zip entries. */
    private Map<String, E> entries;

    /** Maps offsets specified in the ZIP file to real offsets in the file. */
    private PositionMapper mapper = new PositionMapper();

    /** The number of open resources for reading the entries in this ZIP file. */
    private int open;

    /**
     * Reads the given {@code zip} file in order to provide random access
     * to its entries.
     *
     * @param  source the source for reading the ZIP file from.
     * @param  param the parameters for reading the ZIP file.
     * @throws ZipException if the source data is not compatible to the ZIP
     *         File Format Specification.
     * @throws EOFException on unexpected end-of-file.
     * @throws IOException on any I/O error.
     * @see    #recoverLostEntries()
     */
    @CreatesObligation
    protected RawZipFile(
            final Source source,
            final ZipFileParameters<E> param)
    throws ZipException, EOFException, IOException {
        this.param = param;
        final SeekableByteChannel channel = this.channel = source.channel();
        try {
            length = channel.size();
            charset = param.getCharset();
            final @WillNotClose SeekableByteChannel
                    bchannel = new SafeBufferedReadOnlyChannel(channel, length);
            if (!param.getPreambled())
                checkZipFileSignature(bchannel);
            final int numEntries = findCentralDirectory(bchannel, param.getPostambled());
            mountCentralDirectory(bchannel, numEntries);
            if (preamble + postamble >= length) {
                assert 0 == numEntries;
                if (param.getPreambled()) // otherwise already checked
                    checkZipFileSignature(bchannel);
            }
            assert null != channel;
            assert null != charset;
            assert null != entries;
            assert null != mapper;
            // Do NOT close bchannel - would close channel as well!
        } catch (final Throwable ex) {
            try {
                channel.close();
            } catch (final Throwable ex2) {
                ex.addSuppressed(ex2);
            }
            throw ex;
        }
    }

    private void checkZipFileSignature(final SeekableByteChannel channel)
    throws IOException {
        final long sig = PowerBuffer
                .allocate(4)
                .littleEndian()
                .load(channel.position(preamble))
                .getUInt();
        // Constraint: A ZIP file must start with a Local File Header
        // or a (ZIP64) End Of Central Directory Record iff it's emtpy.
        if (LFH_SIG != sig
                  && ZIP64_EOCDR_SIG != sig
                  && EOCDR_SIG != sig)
            throw new ZipException(
                    "Expected Local File Header or (ZIP64) End Of Central Directory Record!");
    }

    /**
     * Positions the file pointer at the first Central File Header.
     * Performs some means to check that this is really a ZIP file.
     * <p>
     * As a side effect, the following fields will get initialized:
     * <ul>
     * <li>{@link #preamble}
     * <li>{@link #postamble}
     * </ul>
     * <p>
     * The following fields may get updated:
     * <ul>
     * <li>{@link #comment}
     * <li>{@link #mapper}
     * </ul>
     *
     * @throws ZipException If the file is not compatible to the ZIP File
     *         Format Specification.
     * @throws IOException On any other I/O error.
     */
    private int findCentralDirectory(
            final SeekableByteChannel channel,
            final boolean postambled)
    throws IOException {
        // Search for End of central directory record.
        final PowerBuffer eocdr = PowerBuffer
                .allocate(EOCDR_MIN_LEN)
                .littleEndian();
        final long max = length - EOCDR_MIN_LEN;
        final long min = !postambled && max >= 0xffff ? max - 0xffff : 0;
        for (long eocdrPos = max; eocdrPos >= min; eocdrPos--) {
            eocdr.rewind().limit(4).load(channel.position(eocdrPos));
            // end of central dir signature    4 bytes  (0x06054b50)
            if (EOCDR_SIG != eocdr.getUInt())
                continue;

            // Process End Of Central Directory Record.
            eocdr.limit(EOCDR_MIN_LEN).load(channel);
            // number of this disk             2 bytes
            long diskNo = eocdr.getUShort();
            // number of the disk with the
            // start of the central directory  2 bytes
            long cdDiskNo = eocdr.getUShort();
            // total number of entries in the
            // central directory on this disk  2 bytes
            long cdEntriesDisk = eocdr.getUShort();
            // total number of entries in
            // the central directory           2 bytes
            long cdEntries = eocdr.getUShort();
            if (0 != diskNo || 0 != cdDiskNo || cdEntriesDisk != cdEntries)
                throw new ZipException(
                        "ZIP file spanning/splitting is not supported!");
            // size of the central directory   4 bytes
            long cdSize = eocdr.getUInt();
            // offset of start of central
            // directory with respect to
            // the starting disk number        4 bytes
            long cdPos = eocdr.getUInt();
            // .ZIP file comment length        2 bytes
            int commentLen = eocdr.getUShort();
            // .ZIP file comment       (variable size)
            if (0 < commentLen)
                comment = PowerBuffer
                        .allocate(commentLen)
                        .load(channel)
                        .array();
            preamble = eocdrPos;
            postamble = length - channel.position();

            // Check for ZIP64 End Of Central Directory Locator.
            final long eocdlPos = eocdrPos - ZIP64_EOCDL_LEN;
            final PowerBuffer zip64eocdl = PowerBuffer
                .allocate(ZIP64_EOCDL_LEN)
                .littleEndian();
            // zip64 end of central dir locator 
            // signature                       4 bytes  (0x07064b50)
            if (0 > eocdlPos || ZIP64_EOCDL_SIG != zip64eocdl
                    .load(channel.position(eocdlPos))
                    .getUInt()) {
                // Seek and check first CFH, probably requiring an offset mapper.
                long offset = eocdrPos - cdSize;
                channel.position(offset);
                offset -= cdPos;
                if (0 != offset)
                    mapper = new OffsetPositionMapper(offset);
                return (int) cdEntries;
            }

            // number of the disk with the
            // start of the zip64 end of 
            // central directory               4 bytes
            final long zip64eocdrDisk = zip64eocdl.getUInt();
            // relative offset of the zip64
            // end of central directory record 8 bytes
            final long zip64eocdrPos = zip64eocdl.getLong();
            // total number of disks           4 bytes
            final long totalDisks = zip64eocdl.getUInt();
            if (0 != zip64eocdrDisk || 1 != totalDisks)
                throw new ZipException(
                        "ZIP file spanning/splitting is not supported!");

            // Read Zip64 End Of Central Directory Record.
            final PowerBuffer zip64eocdr = PowerBuffer
                    .allocate(ZIP64_EOCDR_MIN_LEN)
                    .littleEndian()
                    .load(channel.position(zip64eocdrPos));
            // zip64 end of central dir 
            // signature                       4 bytes  (0x06064b50)
            if (ZIP64_EOCDR_SIG != zip64eocdr.getUInt())
                throw new ZipException(
                        "Expected ZIP64 End Of Central Directory Record!");
            // size of zip64 end of central
            // directory record                8 bytes
            // version made by                 2 bytes
            // version needed to extract       2 bytes
            zip64eocdr.skip(8 + 2 + 2);
            // number of this disk             4 bytes
            diskNo = zip64eocdr.getUInt();
            // number of the disk with the 
            // start of the central directory  4 bytes
            cdDiskNo = zip64eocdr.getUInt();
            // total number of entries in the
            // central directory on this disk  8 bytes
            cdEntriesDisk = zip64eocdr.getLong();
            // total number of entries in the
            // central directory               8 bytes
            cdEntries = zip64eocdr.getLong();
            if (0 != diskNo || 0 != cdDiskNo || cdEntriesDisk != cdEntries)
                throw new ZipException(
                        "ZIP file spanning/splitting is not supported!");
            if (cdEntries < 0 || Integer.MAX_VALUE < cdEntries)
                throw new ZipException(
                        "Total Number Of Entries In The Central Directory out of range!");
            // size of the central directory   8 bytes
            //cdSize = zip64eocdr.getLong();
            zip64eocdr.skip(8);
            // offset of start of central
            // directory with respect to
            // the starting disk number        8 bytes
            cdPos = zip64eocdr.getLong();
            // zip64 extensible data sector    (variable size)
            channel.position(cdPos);
            preamble = zip64eocdrPos;
            return (int) cdEntries;
        }

        // Start recovering file entries from min.
        preamble = min;
        postamble = length - min;
        return 0;
    }

    /**
     * Reads the central directory from the given seekable byte channel and
     * populates the internal tables with ZipEntry instances.
     * <p>
     * The ZipEntrys will know all data that can be obtained from
     * the central directory alone, but not the data that requires the
     * local file header or additional data to be read.
     * <p>
     * As a side effect, the following fields will get initialized:
     * <ul>
     * <li>{@link #entries}
     * </ul>
     * <p>
     * The following fields may get updated:
     * <ul>
     * <li>{@link #preamble}
     * <li>{@link #charset}
     * </ul>
     *
     * @throws ZipException If the file is not compatible to the ZIP File
     *         Format Specification.
     * @throws IOException on any I/O error.
     */
    private void mountCentralDirectory(
            final SeekableByteChannel channel,
            int numEntries)
    throws IOException {
        final PowerBuffer cfh = PowerBuffer
                .allocate(CFH_MIN_LEN)
                .littleEndian();
        final Map<String, E> entries = new LinkedHashMap<>(
                Math.max(initialCapacity(numEntries), 16));
        for (; ; numEntries--) {
            cfh.rewind().limit(4).load(channel);
            // central file header signature   4 bytes  (0x02014b50)
            if (CFH_SIG != cfh.getUInt())
                break;
            cfh.limit(CFH_MIN_LEN).load(channel);
            final int gpbf = cfh.position(8).getUShort();
            final int nameLen = cfh.position(28).getUShort();
            final PowerBuffer name = PowerBuffer
                    .allocate(nameLen)
                    .load(channel);
            // See appendix D of PKWARE's ZIP File Format Specification.
            final boolean utf8 = 0 != (gpbf & GPBF_UTF8);
            if (utf8)
                charset = UTF8;
            final E entry = param.newEntry(decode(name.array()));
            try {
                // central file header signature   4 bytes  (0x02014b50)
                cfh.position(4);
                // version made by                 2 bytes
                entry.setRawPlatform(cfh.getUShort() >> 8);
                // version needed to extract       2 bytes
                // general purpose bit flag        2 bytes
                cfh.skip(2 + 2);
                entry.setGeneralPurposeBitFlags(gpbf);
                // compression method              2 bytes
                entry.setRawMethod(cfh.getUShort());
                // last mod file time              2 bytes
                // last mod file date              2 bytes
                entry.setRawTime(cfh.getUInt());
                // crc-32                          4 bytes
                entry.setRawCrc(cfh.getUInt());
                // compressed size                 4 bytes
                entry.setRawCompressedSize(cfh.getUInt());
                // uncompressed size               4 bytes
                entry.setRawSize(cfh.getUInt());
                // file name length                2 bytes
                cfh.skip(2);
                // extra field length              2 bytes
                final int extraLen = cfh.getUShort();
                // file comment length             2 bytes
                final int commentLen = cfh.getUShort();
                // disk number start               2 bytes
                // internal file attributes        2 bytes
                cfh.skip(2 + 2);
                //entry.setEncodedInternalAttributes(readUShort(cfh, off));
                // external file attributes        4 bytes
                entry.setRawExternalAttributes(cfh.getUInt());
                // relative offset of local header 4 bytes
                long lfhOff = cfh.getUInt();
                entry.setRawOffset(lfhOff); // must be unmapped!
                // extra field (variable size)
                if (0 < extraLen)
                    entry.setRawExtraFields(PowerBuffer
                            .allocate(extraLen)
                            .load(channel)
                            .array());
                // file comment (variable size)
                if (0 < commentLen)
                    entry.setRawComment(decode(PowerBuffer
                            .allocate(commentLen)
                            .load(channel)
                            .array()));
                // Re-load virtual offset after ZIP64 Extended Information
                // Extra Field may have been parsed, map it to the real
                // offset and conditionally update the preamble size from it.
                lfhOff = mapper.map(entry.getOffset());
                if (lfhOff < preamble)
                    preamble = lfhOff;
            } catch (IllegalArgumentException cause) {
                final ZipException ex = new ZipException(entry.getName()
                        + " (invalid ZIP entry)");
                ex.initCause(cause);
                throw ex;
            }

            // Map the entry using the name that has been determined
            // by the ZipEntryFactory.
            // Note that this name may differ from what has been found
            // in the ZIP file!
            entries.put(entry.getName(), entry);
        }

        // Check if the number of entries found matches the number of entries
        // declared in the (ZIP64) End Of Central Directory header.
        // Sometimes, legacy ZIP32 archives (those without ZIP64 extensions)
        // contain more than the maximum number of entries specified in the
        // ZIP File Format Specification, which is 65535 (= 0xffff, a two byte
        // unsigned integer).
        // In this case, the declared number of entries usually overflows and
        // may get negative (Java does not support unsigned integers).
        // Although beyond the spec, we silently tolerate this in the test.
        // Thanks to Jean-Francois Thamie for this hint!
        if (0 != numEntries % 0x10000)
            throw new ZipException(
                    "Expected " +
                    Math.abs(numEntries) +
                    (numEntries > 0 ? " more" : " less") +
                    " entries in the Central Directory!");

        // Commit map of entries.
        this.entries = entries;
    }

    /**
     * Recovers any lost entries which have been added to the ZIP file after
     * the (last) End Of Central Directory Record (EOCDR).
     * This method should be called immediately after the constructor.
     * It requires a fully initialized object, hence it's not part of the
     * constructor.
     * For example, to recoverLostEntries encrypted entries, it may require
     * {@link #getCryptoParameters() crypto parameters}.
     * <p>
     * This method starts parsing entries at the start of the postamble and
     * continues until it hits EOF or any non-entry data.
     * As a side effect, it will not only add any found entries to its internal
     * map, but will also cut the start of the postamble accordingly.
     * <p>
     * Note that it's very likely that this method terminates with an
     * exception unless the postamble is empty or contains only valid ZIP
     * entries.
     * Therefore it may be a good idea to log or silently ignore any exception
     * thrown by this method.
     * If an exception is thrown, you can check and recoverLostEntries the remaining
     * postamble for post-mortem analysis by calling
     * {@link #getPostambleLength()} and {@link #getPostambleInputStream()}.
     * 
     * @return {@code this}
     * @throws ZipException if an invalid entry is found.
     * @throws EOFException on unexpected end-of-file.
     * @throws IOException on any I/O error.
     */
    public RawZipFile<E> recoverLostEntries()
    throws ZipException, EOFException, IOException {
        final SeekableByteChannel
                channel = new SafeBufferedReadOnlyChannel(channel(), length);
        final long length = this.length;
        while (0 < postamble) {
            long pos = length - postamble;
            final PowerBuffer lfh = PowerBuffer
                    .allocate(LFH_MIN_LEN)
                    .littleEndian()
                    .load(channel.position(pos));
            if (LFH_SIG != lfh.getUInt())
                throw new ZipException("Expected Local File Header!");
            final int gpbf = lfh.position(6).getUShort();
            final int nameLen = lfh.position(26).getUShort();
            // See appendix D of PKWARE's ZIP File Format Specification.
            if (0 != (gpbf & GPBF_UTF8))
                charset = UTF8;
            final E entry = param.newEntry(decode(PowerBuffer
                    .allocate(nameLen)
                    .load(channel)
                    .array()));
            // local file header signature     4 bytes  (0x04034b50)
            // version needed to extract       2 bytes
            // general purpose bit flag        2 bytes
            entry.setGeneralPurposeBitFlags(gpbf);
            lfh.position(8);
            // compression method              2 bytes
            entry.setRawMethod(lfh.getUShort());
            // last mod file time              2 bytes
            // last mod file date              2 bytes
            entry.setRawTime(lfh.getUInt());
            // crc-32                          4 bytes
            entry.setRawCrc(lfh.getUInt());
            // compressed size                 4 bytes
            entry.setRawCompressedSize(lfh.getUInt());
            // uncompressed size               4 bytes
            entry.setRawSize(lfh.getUInt());
            // file name length                2 bytes
            lfh.skip(2);
            // extra field length              2 bytes
            final int extraLen = lfh.getUShort();
            entry.setRawOffset(mapper.unmap(pos));
            // extra field (variable size)
            if (0 < extraLen)
                entry.setRawExtraFields(PowerBuffer
                        .allocate(extraLen)
                        .load(channel)
                        .array());

            // Process entry contents.
            if (entry.getGeneralPurposeBitFlag(GPBF_DATA_DESCRIPTOR)) {
                // HC SUNT DRACONES!
                // This is the tough one.
                // We need to process the entry as if we were unzipping
                // it because the CRC-32, the compressed size and the
                // uncompressed size are unknown.
                // Once we have done this, we compare our findings to
                // the Data Descriptor which comes next.
                final long start = pos = channel.position();
                SeekableByteChannel echannel = new IntervalReadOnlyChannel(
                        channel, pos, length - pos);
                WinZipAesEntryExtraField field = null;
                int method = entry.getMethod();
                if (entry.isEncrypted()) {
                    if (WINZIP_AES != method)
                        throw new ZipException(entry.getName()
                                + " (encrypted compression method "
                                + method
                                + " is not supported)");
                    echannel = new WinZipAesEntryReadOnlyChannel(echannel,
                            new WinZipAesEntryParameters(
                                parameters(
                                    WinZipAesParameters.class,
                                    getCryptoParameters()),
                                entry));
                    field = (WinZipAesEntryExtraField)
                            entry.getExtraField(WINZIP_AES_ID);
                    method = field.getMethod();
                }
                final int bufSize = getBufferSize(entry);
                CountingInputStream din = null;
                InputStream in;
                switch (method) {
                    case DEFLATED:
                        in = new ZipInflaterInputStream(
                                new DummyByteChannelInputStream(echannel),
                                bufSize);
                        break;
                    case BZIP2:
                        din = new CountingInputStream(
                                new ChannelInputStream(echannel));
                        in = new BZip2CompressorInputStream(din);
                        break;
                    default:
                        throw new ZipException(entry.getName()
                                + " (compression method "
                                + method
                                + " is not supported)");
                }
                try (final CheckedInputStream cin = new CheckedInputStream(in, new CRC32())) {
                    entry.setRawSize(cin.skip(Long.MAX_VALUE));
                    if (null != field && field.getVendorVersion() == VV_AE_2)
                        entry.setRawCrc(0);
                    else
                        entry.setRawCrc(cin.getChecksum().getValue());
                    // Sync file pointer on deflated input again.
                    switch (method) {
                        case DEFLATED:
                            final Inflater inf = ((ZipInflaterInputStream) in)
                                    .getInflater();
                            assert inf.finished();
                            pos += inf.getBytesRead();
                            break;
                        case BZIP2:
                            pos += din.getBytesRead();
                            break;
                        default:
                            throw new AssertionError();
                    }
                }
                if (null != field)
                    pos += overhead(field.getKeyStrength());
                entry.setRawCompressedSize(pos - start);

                // We have reconstituted all meta data for the entry.
                // Next comes the Data Descriptor.
                // Let's parse and check it.
                final PowerBuffer dd = PowerBuffer
                        .allocate(entry.isZip64ExtensionsRequired()
                            ? 4 + 8 + 8
                            : 4 + 4 + 4)
                        .littleEndian()
                        .limit(4)
                        .load(channel.position(pos));
                long crc = dd.getUInt();
                // Note the Data Descriptor's Signature is optional:
                // All newer apps should write it (and so does TrueZIP),
                // but older apps might not.
                if (DD_SIG == crc)
                    dd.rewind();
                dd.limit(dd.capacity()).load(channel);
                crc = dd.position(0).getUInt();
                final long csize;
                final long size;
                if (entry.isZip64ExtensionsRequired()) {
                    csize = dd.getLong();
                    size  = dd.getLong();
                } else {
                    csize = dd.getUInt();
                    size  = dd.getUInt();
                }
                if (entry.getCrc() != crc)
                    throw new Crc32Exception(entry.getName(),
                            entry.getCrc(), crc);
                if (entry.getCompressedSize() != csize)
                    throw new ZipException(entry.getName()
                            + " (invalid compressed size in Data Descriptor)");
                if (entry.getSize() != size)
                    throw new ZipException(entry.getName()
                            + " (invalid uncompressed size in Data Descriptor)");
            } else {
                // This is the easy one.
                // The entry is not using a Data Descriptor, so we can
                // use the properties parsed from the Local File Header.
                pos += entry.getCompressedSize();
                channel.position(pos - 1);
                if (pos > length || channel.position() >= channel.size())
                    throw new ZipException(entry.getName()
                            + " (truncated ZIP entry)");
            }

            // Entry is almost recovered. Update the postamble length.
            postamble = length - channel.position();

            // Map the entry using the name that has been determined
            // by the ZipEntryFactory.
            // Note that this name may differ from what has been found
            // in the ZIP file!
            entries.put(entry.getName(), entry);
        }
        return this;
    }

    final Map<String, E> getRawEntries() {
        return entries;
    }

    private String decode(byte[] buffer) {
        return new String(buffer, charset);
    }

    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    @CheckForNull final byte[] getRawComment() {
        return this.comment;
    }

    /**
     * Returns the file comment.
     * 
     * @return The file comment.
     */
    public @Nullable String getComment() {
        final byte[] comment = this.comment;
        return null == comment ? null : decode(comment);
    }

    /**
     * Returns {@code true} if and only if this ZIP file is busy reading
     * one or more entries.
     */
    public boolean busy() {
        return 0 < open;
    }

    /**
     * Returns the character set which is effectively used for
     * decoding entry names and the file comment.
     * Depending on the ZIP file contents, this may differ from the character
     * set provided to the constructor.
     */
    public Charset getRawCharset() {
        return charset;
    }

    /**
     * Returns the name of the character set which is effectively used for
     * decoding entry names and the file comment.
     * Depending on the ZIP file contents, this may differ from the character
     * set provided to the constructor.
     */
    public String getCharset() {
        return charset.name();
    }

    /**
     * Returns the number of entries in this ZIP file.
     */
    public int size() {
        return entries.size();
    }

    /**
     * Returns an iteration of all entries in this ZIP file.
     * Note that the iterated entries are shared with this instance.
     * It is illegal to change their state!
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
     * Returns the file length of this ZIP file in bytes.
     */
    public long length() {
        return length;
    }

    /**
     * Returns the size of the preamble of this ZIP file in bytes.
     *
     * @return A positive value or zero to indicate that this ZIP file does
     *         not have a preamble.
     */
    public long getPreambleLength() {
        return preamble;
    }

    /**
     * Returns an {@link InputStream} to load the preamble of this ZIP file.
     * <p>
     * Note that the returned stream is a <i>lightweight</i> stream,
     * i.e. there is no external resource such as a {@link SeekableByteChannel}
     * allocated for it. Instead, all streams returned by this method share
     * the underlying {@code SeekableByteChannel} of this {@code ZipFile}.
     * This allows to close this object (and hence the underlying
     * {@code SeekableByteChannel}) without cooperation of the returned
     * streams, which is important if the client application wants to work on
     * the underlying file again (e.g. update or delete it).
     *
     * @throws ZipException If this ZIP file has been closed.
     */
    @CreatesObligation
    public InputStream getPreambleInputStream() throws IOException {
        return new ChannelInputStream(
                new EntryReadOnlyChannel(0, preamble));
    }

    /**
     * Returns the size of the postamble of this ZIP file in bytes.
     *
     * @return A positive value or zero to indicate that this ZIP file does
     *         not have a postamble.
     */
    public long getPostambleLength() {
        return postamble;
    }

    /**
     * Returns an {@link InputStream} to load the postamble of this ZIP file.
     * <p>
     * Note that the returned stream is a <i>lightweight</i> stream,
     * i.e. there is no external resource such as a {@link SeekableByteChannel}
     * allocated for it. Instead, all streams returned by this method share
     * the underlying {@code SeekableByteChannel} of this {@code ZipFile}.
     * This allows to close this object (and hence the underlying
     * {@code SeekableByteChannel}) without cooperation of the returned
     * streams, which is important if the client application wants to work on
     * the underlying file again (e.g. update or delete it).
     *
     * @throws ZipException If this ZIP file has been closed.
     */
    @CreatesObligation
    public InputStream getPostambleInputStream() throws IOException {
        channel();
        return new ChannelInputStream(
                new EntryReadOnlyChannel(length - postamble, postamble));
    }

    final PositionMapper getOffsetMapper() {
        return mapper;
    }

    /**
     * Returns {@code true} if and only if the offsets in this ZIP file
     * are relative to the start of the file, rather than the first Local
     * File Header.
     * <p>
     * This method is intended for very special purposes only.
     */
    public boolean offsetsConsiderPreamble() {
        assert mapper != null;
        return 0 == mapper.map(0);
    }

    /**
     * Returns the parameters for encryption or authentication of entries.
     * 
     * Returns The parameters for encryption or authentication of entries.
     */
    protected abstract @CheckForNull ZipCryptoParameters getCryptoParameters();

    /**
     * Equivalent to {@link #getInputStream(String, Boolean, boolean)
     * getInputStream(name, null, true)}.
     */
    @CreatesObligation
    public final @Nullable InputStream getInputStream(String name)
    throws IOException {
        return getInputStream(name, null, true);
    }

    /**
     * Equivalent to {@link #getInputStream(String, Boolean, boolean)
     * getInputStream(entry.getName(), null, true)} instead.
     */
    @CreatesObligation
    public final @Nullable InputStream getInputStream(ZipEntry entry)
    throws IOException {
        return getInputStream(entry.getName(), null, true);
    }

    /**
     * Equivalent to {@link #getInputStream(String, Boolean, boolean)
     * getInputStream(name, true, true)}.
     */
    @CreatesObligation
    public final @Nullable InputStream getCheckedInputStream(String name)
    throws IOException {
        return getInputStream(name, true, true);
    }

    /**
     * Equivalent to {@link #getInputStream(String, Boolean, boolean)
     * getInputStream(entry.getName(), true, true)} instead.
     */
    @CreatesObligation
    public final @Nullable InputStream getCheckedInputStream(ZipEntry entry)
    throws IOException {
        return getInputStream(entry.getName(), true, true);
    }

    /**
     * Returns an {@code InputStream} for reading the contents of the given
     * entry.
     * <p>
     * If the {@link #close} method is called on this instance, all input
     * streams returned by this method are closed, too.
     *
     * @param  name The name of the entry to get the stream for.
     * @param  check Whether or not the entry content gets checked/authenticated.
     *         If the parameter {@code process} is {@code false}, then this
     *         parameter is ignored.
     *         Otherwise, if this parameter is {@code null}, then it is set to
     *         the {@link ZipEntry#isEncrypted()} property of the given entry.
     *         Finally, if this parameter is {@code true},
     *         then the following additional check is performed for the entry:
     *         <ul>
     *         <li>If the entry is encrypted, then the Message Authentication
     *             Code (MAC) value gets computed and checked.
     *             If this check fails, then a
     *             {@link ZipAuthenticationException} gets thrown from this
     *             method (pre-check).
     *         <li>If the entry is <em>not</em> encrypted, then the CRC-32
     *             value gets computed and checked.
     *             First, the local file header is checked to hold the same
     *             CRC-32 value than the central directory record.
     *             Second, the CRC-32 value is computed and checked.
     *             If this check fails, then a {@link Crc32Exception}
     *             gets thrown when {@link InputStream#close} is called on the
     *             returned entry stream (post-check).
     *         </ul>
     * @param  process Whether or not the entry contents should get processed,
     *         e.g. inflated.
     *         This should be set to {@code false} if and only if the
     *         application is going to copy entries from an input ZIP file to
     *         an output ZIP file.
     * @return A stream to read the entry data from or {@code null} if the
     *         entry does not exist.
     * @throws ZipAuthenticationException If the entry is encrypted and
     *         checking the MAC fails.
     * @throws ZipException If this file is not compatible to the ZIP File
     *         Format Specification.
     * @throws IOException If the entry cannot get read from this ZipFile.
     */
    @CreatesObligation
    protected @Nullable InputStream getInputStream(
            final String name,
            @CheckForNull Boolean check,
            final boolean process)
    throws ZipException, IOException {
        final SeekableByteChannel channel = channel();
        Objects.requireNonNull(name);
        final ZipEntry entry = entries.get(name);
        if (null == entry)
            return null;
        long pos = entry.getOffset();
        assert UNKNOWN != pos;
        pos = mapper.map(pos);
        final PowerBuffer lfh = PowerBuffer
                .allocate(LFH_MIN_LEN)
                .littleEndian()
                .load(channel.position(pos));
        if (LFH_SIG != lfh.getUInt())
            throw new ZipException(name + " (expected Local File Header)");
        lfh.position(LFH_FILE_NAME_LENGTH_POS);
        pos += LFH_MIN_LEN
                + lfh.getUShort() // file name length
                + lfh.getUShort(); // extra field length
        SeekableByteChannel echannel = new EntryReadOnlyChannel(
                pos, entry.getCompressedSize());
        try {
            if (!process) {
                assert UNKNOWN != entry.getCrc();
                return new ChannelInputStream(echannel);
            }
            if (null == check)
                check = entry.isEncrypted();
            int method = entry.getMethod();
            if (entry.isEncrypted()) {
                if (WINZIP_AES != method)
                    throw new ZipException(name
                            + " (encrypted compression method "
                            + method
                            + " is not supported)");
                final WinZipAesEntryReadOnlyChannel
                        eechannel = new WinZipAesEntryReadOnlyChannel(echannel,
                                new WinZipAesEntryParameters(
                                    parameters(
                                        WinZipAesParameters.class,
                                        getCryptoParameters()),
                                    entry));
                echannel = eechannel;
                if (check) {
                    eechannel.authenticate();
                    // Disable redundant CRC-32 check.
                    check = false;
                }
                final WinZipAesEntryExtraField field
                        = (WinZipAesEntryExtraField) entry.getExtraField(WINZIP_AES_ID);
                method = field.getMethod();
            }
            if (check) {
                // Check CRC32 in the Local File Header or Data Descriptor.
                long localCrc;
                if (entry.getGeneralPurposeBitFlag(GPBF_DATA_DESCRIPTOR)) {
                    // The CRC32 is in the Data Descriptor after the compressed
                    // size.
                    // Note the Data Descriptor's Signature is optional:
                    // All newer apps should write it (and so does TrueZIP),
                    // but older apps might not.
                    final PowerBuffer dd = PowerBuffer
                            .allocate(8)
                            .littleEndian()
                            .load(channel.position(pos + entry.getCompressedSize()));
                    localCrc = dd.getUInt();
                    if (DD_SIG == localCrc)
                        localCrc = dd.getUInt();
                } else {
                    // The CRC32 in the Local File Header.
                    localCrc = lfh.position(14).getUInt();
                }
                if (entry.getCrc() != localCrc)
                    throw new Crc32Exception(name, entry.getCrc(), localCrc);
            }
            final int bufSize = getBufferSize(entry);
            InputStream in;
            switch (method) {
                case STORED:
                    in = new ChannelInputStream(echannel);
                    break;
                case DEFLATED:
                    in = new ZipInflaterInputStream(
                            new DummyByteChannelInputStream(echannel),
                            bufSize);
                    break;
                case BZIP2:
                    in = new BZip2CompressorInputStream(
                            new ChannelInputStream(echannel));
                    break;
                default:
                    throw new ZipException(name
                            + " (compression method "
                            + method
                            + " is not supported)");
            }
            if (check)
                in = new Crc32InputStream(in, entry, bufSize);
            return in;
        } catch (final Throwable ex) {
            try {
                echannel.close();
            } catch (final Throwable ex2) {
                ex.addSuppressed(ex2);
            }
            throw ex;
        }
    }

    private static int getBufferSize(final ZipEntry entry) {
        long size = entry.getSize();
        if (MAX_FLATER_BUF_LENGTH < size)
            size = MAX_FLATER_BUF_LENGTH;
        else if (size < MIN_FLATER_BUF_LENGTH)
            size = MIN_FLATER_BUF_LENGTH;
        return (int) size;
    }

    /** Asserts that this ZIP file is still open for reading its entries. */
    private SeekableByteChannel channel() throws ZipException {
        final SeekableByteChannel channel = this.channel;
        if (null == channel)
            throw new ZipException("ZIP file closed!");
        return channel;
    }

    /**
     * Closes the file.
     * This closes any allocate input streams reading from this ZIP file.
     *
     * @throws IOException if an error occurs closing the file.
     */
    @Override
    @DischargesObligation
    public void close() throws IOException {
        final SeekableByteChannel channel = this.channel;
        if (null == channel)
            return;
        channel.close();
        this.channel = null;
    }

    /**
     * An interval read-only channel which accounts for itself until it gets
     * closed.
     * Note that when an object of this class gets closed, the decorated
     * read-only channel, i.e. the raw file does NOT get closed!
     */
    private final class EntryReadOnlyChannel extends DecoratingReadOnlyChannel {
        boolean closed;

        @CreatesObligation
        EntryReadOnlyChannel(final long start, final long size)
        throws IOException {
            super(new IntervalReadOnlyChannel(channel(), start, size));
            RawZipFile.this.open++;
        }

        @Override
        public void close() throws IOException {
            if (closed)
                return;
            // Never close the channel!
            //super.close();
            RawZipFile.this.open--;
            closed = true;
        }
    } // EntryReadOnlyChannel

    /**
     * A buffered load only file which is safe for use with a concurrently
     * growing file, e.g. when another thread is appending to it.
     */
    private static final class SafeBufferedReadOnlyChannel
    extends BufferedReadOnlyChannel {
        final long size;

        @CreatesObligation
        SafeBufferedReadOnlyChannel(
                final @WillCloseWhenClosed SeekableByteChannel channel,
                final long size) {
            super(channel);
            this.size = size;
        }

        @Override
        public long size() throws IOException {
            checkOpen();
            return size;
        }
    } // SafeBufferedReadOnlyChannel
}
