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

import de.schlichtherle.truezip.rof.BufferedReadOnlyFile;
import de.schlichtherle.truezip.rof.IntervalReadOnlyFile;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.rof.ReadOnlyFileInputStream;
import de.schlichtherle.truezip.util.Pool;
import static de.schlichtherle.truezip.zip.Constants.*;
import static de.schlichtherle.truezip.zip.LittleEndian.*;
import static de.schlichtherle.truezip.zip.WinZipAesEntryExtraField.*;
import static de.schlichtherle.truezip.zip.ZipEntry.*;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipException;
import net.jcip.annotations.NotThreadSafe;

/**
 * Provides unsafe (raw) access to a ZIP file using unsynchronized methods and
 * shared {@link ZipEntry} instances.
 * <p>
 * <b>Warning:</b> This class is <em>not</em> intended for public use
 * - its API may change at will without prior notification!
 * <p>
 * Where the constructors of this class accept a {@code charset}
 * parameter, this is used to decode comments and entry names in the ZIP file.
 * However, if an entry has bit 11 set in its General Purpose Bit Flags,
 * then this parameter is ignored and "UTF-8" is used for this entry.
 * This is in accordance to Appendix D of PKWARE's ZIP File Format
 * Specification, version 6.3.0 and later.
 * <p>
 * This class is able to skip a preamble like the one found in self extracting
 * archives.
 *
 * @see     RawZipOutputStream
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
@DefaultAnnotation(NonNull.class)
public abstract class RawZipFile<E extends ZipEntry>
implements Iterable<E>, Closeable {

    private static final int LFH_FILE_NAME_LENGTH_OFF =
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

    /** Maps entry names to zip entries. */
    private final Map<String, E> entries = new LinkedHashMap<String, E>();

    /** The charset to use for entry names and comments. */
    private Charset charset;

    /** The encoded file comment. */
    private @CheckForNull byte[] comment;

    /** The total number of bytes in this ZIP file. */
    private long length = -1;

    /** The number of bytes in the preamble of this ZIP file. */
    private long preamble;

    /** The number of bytes in the postamble of this ZIP file. */
    private long postamble;

    /** Maps offsets specified in the ZIP file to real offsets in the file. */
    private OffsetMapper mapper;

    private final ZipEntryFactory<E> factory;

    /** The nullable data source. */
    private @CheckForNull ReadOnlyFile archive;

    /** The number of allocate streams reading from this ZIP file. */
    private int openEntries;

    /**
     * Reads the given {@code archive} in order to provide random access
     * to its ZIP entries.
     *
     * @param  archive the {@link ReadOnlyFile} instance to be read in order to
     *         provide random access to its ZIP entries.
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
     * @param  factory a factory for {@link ZipEntry}s.
     * @throws FileNotFoundException if {@code archive} cannot get opened for
     *         reading.
     * @throws ZipException if {@code archive} is not compatible to the ZIP
     *         File Format Specification.
     * @throws IOException on any other I/O related issue.
     */
    protected RawZipFile(
            ReadOnlyFile archive,
            Charset charset,
            boolean preambled,
            boolean postambled,
            ZipEntryFactory<E> factory)
    throws IOException {
        this(   new SingleReadOnlyFilePool(archive),
                charset, preambled, postambled, factory);
    }

    RawZipFile(
            final Pool<ReadOnlyFile, IOException> source,
            final Charset charset,
            final boolean preambled,
            final boolean postambled,
            final ZipEntryFactory<E> factory)
    throws IOException {
        if (null == charset || null == factory)
            throw new NullPointerException();
        final ReadOnlyFile rof = source.allocate();
        try {
            this.archive = rof;
            this.charset = charset;
            this.factory = factory;
            final BufferedReadOnlyFile brof;
            if (rof instanceof BufferedReadOnlyFile)
                brof = (BufferedReadOnlyFile) rof;
            else
                brof = new BufferedReadOnlyFile(rof);
            mountCentralDirectory(brof, preambled, postambled);
            // Do NOT close brof - would close rof as well!
        } catch (IOException ex) {
            source.release(rof);
            throw ex;
        }
        assert null != this.archive;
        assert null != this.charset;
        //assert null != this.factory; // pleases FindBugs!
        assert null != this.mapper;
    }

    /**
     * Reads the central directory of the given file and populates
     * the internal tables with ZipEntry instances.
     * <p>
     * The ZipEntrys will know all data that can be obtained from
     * the central directory alone, but not the data that requires the
     * local file header or additional data to be read.
     *
     * @throws ZipException If the file is not compatible to the ZIP File
     *         Format Specification.
     * @throws IOException On any other I/O related issue.
     */
    private void mountCentralDirectory(
            final ReadOnlyFile rof,
            final boolean preambled,
            final boolean postambled)
    throws IOException {
        int numEntries = findCentralDirectory(rof, preambled, postambled);
        assert this.mapper != null;
        this.preamble = Long.MAX_VALUE;
        final byte[] sig = new byte[4];
        final byte[] cfh = new byte[CFH_MIN_LEN - sig.length];
        for (; ; numEntries--) {
            rof.readFully(sig);
            if (CFH_SIG != readUInt(sig, 0))
                break;
            rof.readFully(cfh);
            final int general = readUShort(cfh, 4);
            final int nameLen = readUShort(cfh, 24);
            final byte[] name = new byte[nameLen];
            rof.readFully(name);
            // See appendix D of PKWARE's ZIP File Format Specification.
            final boolean utf8 = 0 != (general & GPBF_UTF8);
            if (utf8)
                this.charset = UTF8;
            final E entry = this.factory.newEntry(decode(name));
            try {
                int off = 0;
                final int versionMadeBy = readUShort(cfh, off);
                off += 2;
                entry.setEncodedPlatform(versionMadeBy >> 8);
                off += 2; // Version Needed To Extract
                entry.setGeneralPurposeBitFlags(general);
                off += 2; // General Purpose Bit Flags
                assert entry.getGeneralPurposeBitFlag(GPBF_UTF8) == utf8;
                entry.setEncodedMethod(readUShort(cfh, off));
                off += 2;
                entry.setEncodedTime(readUInt(cfh, off));
                off += 4;
                entry.setEncodedCrc(readUInt(cfh, off));
                off += 4;
                entry.setEncodedCompressedSize(readUInt(cfh, off));
                off += 4;
                entry.setEncodedSize(readUInt(cfh, off));
                off += 4;
                off += 2;   // File Name Length
                final int extraLen = readUShort(cfh, off);
                off += 2;
                final int commentLen = readUShort(cfh, off);
                off += 2;
                off += 2;   // Disk Number
                //entry.setInternalAttributes(readUShort(cfh, off));
                off += 2;
                //entry.setExternalAttributes(readUInt(cfh, off));
                off += 4;
                // Relative Offset Of Local File Header.
                long lfhOff = readUInt(cfh, off);
                //off += 4;
                entry.setEncodedOffset(lfhOff); // must be unmapped!
                if (0 < extraLen) {
                    final byte[] extra = new byte[extraLen];
                    rof.readFully(extra);
                    entry.setEncodedExtraFields(extra);
                }
                if (0 < commentLen) {
                    final byte[] comment = new byte[commentLen];
                    rof.readFully(comment);
                    entry.setDecodedComment(decode(comment));
                }
                // Re-read virtual offset after ZIP64 Extended Information
                // Extra Field may have been parsed, map it to the real
                // offset and conditionally update the preamble size from it.
                lfhOff = this.mapper.location(entry.getOffset());
                if (lfhOff < this.preamble)
                    this.preamble = lfhOff;
            } catch (RuntimeException incompatibleZipFile) {
                final ZipException ex = new ZipException(entry.getName());
                ex.initCause(incompatibleZipFile);
                throw ex;
            }

            // Map the entry using the name that has been determined
            // by the ZipEntryFactory.
            // Note that this name may differ from what has been found
            // in the ZIP file!
            this.entries.put(entry.getName(), entry);
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
        if (numEntries % 0x10000 != 0)
            throw new ZipException(
                    "Expected " +
                    Math.abs(numEntries) +
                    (numEntries > 0 ? " more" : " less") +
                    " entries in the Central Directory!");
        if (this.preamble == ULong.MAX_VALUE)
            this.preamble = 0;
    }

    private String decode(byte[] bytes) {
        return new String(bytes, charset);
    }

    /**
     * Positions the file pointer at the first Central File Header.
     * Performs some means to check that this is really a ZIP file.
     * <p>
     * As a side effect, both {@code mapper} and {@code postamble}
     * will be set.
     *
     * @throws ZipException If the file is not compatible to the ZIP File
     *         Format Specification.
     * @throws IOException On any other I/O error.
     */
    private int findCentralDirectory(
            final ReadOnlyFile rof,
            boolean preambled,
            final boolean postambled)
    throws ZipException, IOException {
        final byte[] sig = new byte[4];
        if (!preambled) {
            rof.seek(0);
            rof.readFully(sig);
            final long signature = readUInt(sig, 0);
            // Constraint: A ZIP file must start with a Local File Header
            // or a (ZIP64) End Of Central Directory Record iff it's emtpy.
            preambled = signature == LFH_SIG
                      || signature == ZIP64_EOCDR_SIG
                      || signature == EOCDR_SIG;
        }
        if (preambled) {
            this.length = rof.length();
            final long max = this.length - EOCDR_MIN_LEN;
            final long min = !postambled && max >= 0xffff ? max - 0xffff : 0;
            for (long eocdrOffset = max; eocdrOffset >= min; eocdrOffset--) {
                rof.seek(eocdrOffset);
                rof.readFully(sig);
                if (readUInt(sig, 0) != EOCDR_SIG)
                    continue;
                long diskNo;        // number of this disk
                long cdDiskNo;      // number of the disk with the start of the central directory
                long cdEntriesDisk; // total number of entries in the central directory on this disk
                long cdEntries;     // total number of entries in the central directory
                long cdSize;        // size of the central directory
                long cdOffset;      // offset of start of central directory with respect to the starting disk number
                int commentLen;     // .ZIP file comment length
                int off = 0;
                // Process EOCDR.
                final byte[] eocdr = new byte[EOCDR_MIN_LEN - sig.length];
                rof.readFully(eocdr);
                diskNo = readUShort(eocdr, off);
                off += 2;
                cdDiskNo = readUShort(eocdr, off);
                off += 2;
                cdEntriesDisk = readUShort(eocdr, off);
                off += 2;
                cdEntries = readUShort(eocdr, off);
                off += 2;
                if (diskNo != 0 || cdDiskNo != 0 || cdEntriesDisk != cdEntries)
                    throw new ZipException(
                            "ZIP file spanning/splitting is not supported!");
                cdSize = readUInt(eocdr, off);
                off += 4;
                cdOffset = readUInt(eocdr, off);
                off += 4;
                commentLen = readUShort(eocdr, off);
                //off += 2;
                if (0 < commentLen) {
                    final byte[] comment = new byte[commentLen];
                    rof.readFully(comment);
                    this.comment = comment;
                }
                this.postamble = this.length - rof.getFilePointer();
                // Check for ZIP64 End Of Central Directory Locator.
                try {
                    // Read Zip64 End Of Central Directory Locator.
                    final byte[] zip64eocdl = new byte[ZIP64_EOCDL_LEN];
                    rof.seek(eocdrOffset - ZIP64_EOCDL_LEN);
                    rof.readFully(zip64eocdl);
                    off = 0; // reuse
                    final long zip64eocdlSig = readUInt(zip64eocdl, off);
                    off += 4;
                    if (zip64eocdlSig != ZIP64_EOCDL_SIG)
                        throw new IOException( // MUST be IOException, not ZipException - see catch clauses!
                                "No ZIP64 End Of Central Directory Locator signature found!");
                    final long zip64eocdrDisk;      // number of the disk with the start of the zip64 end of central directory record
                    final long zip64eocdrOffset;    // relative offset of the zip64 end of central directory record
                    final long totalDisks;          // total number of disks
                    zip64eocdrDisk = readUInt(zip64eocdl, off);
                    off += 4;
                    zip64eocdrOffset = readLong(zip64eocdl, off);
                    off += 8;
                    totalDisks = readUInt(zip64eocdl, off);
                    //off += 4;
                    if (zip64eocdrDisk != 0 || totalDisks != 1)
                        throw new ZipException( // MUST be ZipException, not IOException - see catch clauses!
                                "ZIP file spanning/splitting is not supported!");
                    // Read Zip64 End Of Central Directory Record.
                    final byte[] zip64eocdr = new byte[ZIP64_EOCDR_MIN_LEN];
                    rof.seek(zip64eocdrOffset);
                    rof.readFully(zip64eocdr);
                    off = 0; // reuse
                    final long zip64eocdrSig = readUInt(zip64eocdr, off);
                    off += 4;
                    if (zip64eocdrSig != ZIP64_EOCDR_SIG)
                        throw new ZipException( // MUST be ZipException, not IOException - see catch clauses!
                                "No ZIP64 End Of Central Directory Record signature found!");
                    //final long zip64eocdrSize;  // Size Of ZIP64 End Of Central Directory Record
                    //final int madeBy;           // Version Made By
                    //final int needed2extract;   // Version Needed To Extract
                    //zip64eocdrSize = readLong(zip64eocdr, off);
                    off += 8;
                    //madeBy = readUShort(zip64eocdr, off);
                    off += 2;
                    //needed2extract = readUShort(zip64eocdr, off);
                    off += 2;
                    diskNo = readUInt(zip64eocdr, off);
                    off += 4;
                    cdDiskNo = readUInt(zip64eocdr, off);
                    off += 4;
                    cdEntriesDisk = readLong(zip64eocdr, off);
                    off += 8;
                    cdEntries = readLong(zip64eocdr, off);
                    off += 8;
                    if (diskNo != 0 || cdDiskNo != 0 || cdEntriesDisk != cdEntries)
                        throw new ZipException( // MUST be ZipException, not IOException - see catch clauses!
                                "ZIP file spanning/splitting is not supported!");
                    if (cdEntries < 0 || Integer.MAX_VALUE < cdEntries)
                        throw new ZipException( // MUST be ZipException, not IOException - see catch clauses!
                                "Total Number Of Entries In The Central Directory out of range!");
                    cdSize = readLong(zip64eocdr, off);
                    off += 8;
                    cdOffset = readLong(zip64eocdr, off);
                    //off += 8;
                    rof.seek(cdOffset);
                    this.mapper = new OffsetMapper();
                } catch (ZipException ze) {
                    throw ze;
                } catch (IOException ioe) {
                    // Seek and check first CFH, probably using an offset mapper.
                    long start = eocdrOffset - cdSize;
                    rof.seek(start);
                    start -= cdOffset;
                    this.mapper = 0 != start
                            ? new IrregularOffsetMapper(start)
                            : new OffsetMapper();
                }
                return (int) cdEntries;
            }
        }
        throw new ZipException(
                "No End Of Central Directory Record signature found!");
    }

    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    @CheckForNull byte[] getRawComment() {
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
        return 0 < openEntries;
    }

    /**
     * Returns the character set which is effectively used for
     * decoding entry names and the file comment.
     * Depending on the ZIP file contents, this may differ from the character
     * set provided to the constructor.
     * 
     * @since TrueZIP 7.3
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
     * Note that the iteration supports element removal and the returned
     * entries are shared with this instance.
     * It is illegal to change their state!
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
     * Returns an {@link InputStream} to read the preamble of this ZIP file.
     * <p>
     * Note that the returned stream is a <i>lightweight</i> stream,
     * i.e. there is no external resource such as a {@link ReadOnlyFile}
     * allocated for it. Instead, all streams returned by this method share
     * the underlying {@code ReadOnlyFile} of this {@code ZipFile}.
     * This allows to close this object (and hence the underlying
     * {@code ReadOnlyFile}) without cooperation of the returned
     * streams, which is important if the client application wants to work on
     * the underlying file again (e.g. update or delete it).
     *
     * @throws ZipException If this ZIP file has been closed.
     */
    public InputStream getPreambleInputStream() throws IOException {
        assertOpen();
        return new ReadOnlyFileInputStream(
                new EntryReadOnlyFile(0, preamble));
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
     * Returns an {@link InputStream} to read the postamble of this ZIP file.
     * <p>
     * Note that the returned stream is a <i>lightweight</i> stream,
     * i.e. there is no external resource such as a {@link ReadOnlyFile}
     * allocated for it. Instead, all streams returned by this method share
     * the underlying {@code ReadOnlyFile} of this {@code ZipFile}.
     * This allows to close this object (and hence the underlying
     * {@code ReadOnlyFile}) without cooperation of the returned
     * streams, which is important if the client application wants to work on
     * the underlying file again (e.g. update or delete it).
     *
     * @throws ZipException If this ZIP file has been closed.
     */
    public InputStream getPostambleInputStream() throws IOException {
        assertOpen();
        return new ReadOnlyFileInputStream(
                new EntryReadOnlyFile(length - postamble, postamble));
    }

    OffsetMapper getOffsetMapper() {
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
        return 0 == mapper.location(0);
    }

    /**
     * Returns the crypto parameters.
     * 
     * @return The crypto parameters.
     * @since  TrueZIP 7.3
     */
    protected abstract @CheckForNull ZipCryptoParameters getCryptoParameters();

    /**
     * Equivalent to {@link #getInputStream(String, boolean, boolean)
     * getInputStream(name, false, true)}.
     */
    public final @Nullable InputStream getInputStream(String name)
    throws IOException {
        return getInputStream(name, false, true);
    }

    /**
     * Equivalent to {@link #getInputStream(String, boolean, boolean)
     * getInputStream(entry.getName(), false, true)} instead.
     */
    public final @Nullable InputStream getInputStream(ZipEntry entry)
    throws IOException {
        return getInputStream(entry.getName(), false, true);
    }

    /**
     * Equivalent to {@link #getInputStream(String, boolean, boolean)
     * getInputStream(name, true, true)}.
     */
    public final @Nullable InputStream getCheckedInputStream(String name)
    throws IOException {
        return getInputStream(name, true, true);
    }

    /**
     * Equivalent to {@link #getInputStream(String, boolean, boolean)
     * getInputStream(entry.getName(), true, true)} instead.
     */
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
     * @param  check Whether or not the entry's CRC-32 value gets checked.
     *         If {@code process} and this parameter are both {@code true},
     *         then two additional checks are performed for the ZIP entry:
     *         <ol>
     *         <li>All entry headers are checked to have consistent
     *             declarations of the CRC-32 value for the inflated entry
     *             data.
     *         <li>When calling {@link InputStream#close} on the returned entry
     *             stream, the CRC-32 value computed from the inflated entry
     *             data is checked against the declared CRC-32 values.
     *             This is independent from the {@code inflate} parameter.
     *         </ol>
     *         If any of these checks fail, a {@link CRC32Exception} is thrown.
     *         <p>
     *         This parameter should be {@code false} for most
     *         applications, and is the default for the sibling of this class
     *         in {@link java.util.zip.ZipFile java.util.zip.ZipFile}.
     * @param  process Whether or not the entry contents should get processed,
     *         e.g. inflated.
     *         This should be set to {@code false} if and only if the
     *         application is going to copy entries from an input ZIP file to
     *         an output ZIP file.
     * @return A stream to read the entry data from or {@code null} if the
     *         entry does not exist.
     * @throws CRC32Exception If the declared CRC-32 values of the inflated
     *         entry data are inconsistent across the entry headers.
     * @throws ZipException If this file is not compatible to the ZIP File
     *         Format Specification.
     * @throws IOException If the entry cannot get read from this ZipFile.
     */
    protected @Nullable InputStream getInputStream(
            final String name,
            boolean check,
            final boolean process)
    throws IOException {
        assertOpen();
        if (name == null)
            throw new NullPointerException();
        final ZipEntry entry = entries.get(name);
        if (entry == null)
            return null;
        long offset = entry.getOffset();
        assert UNKNOWN != offset;
        // This offset has been set by mountCentralDirectory()
        // and needs to be resolved first.
        offset = mapper.location(offset);
        final ReadOnlyFile archive = this.archive;
        assert null != archive;
        archive.seek(offset);
        final byte[] lfh = new byte[LFH_MIN_LEN];
        archive.readFully(lfh);
        final long lfhSig = readUInt(lfh, 0);
        if (lfhSig != LFH_SIG)
            throw new ZipException(name
            + " (expected Local File Header Signature)");
        offset += LFH_MIN_LEN
                + readUShort(lfh, LFH_FILE_NAME_LENGTH_OFF) // file name length
                + readUShort(lfh, LFH_FILE_NAME_LENGTH_OFF + 2); // extra field length
        ReadOnlyFile rof = new EntryReadOnlyFile(
                offset, entry.getCompressedSize());
        if (!process) {
            assert UNKNOWN != entry.getCrc();
            return new ReadOnlyFileInputStream(rof);
        }
        int method = entry.getMethod();
        if (entry.isEncrypted()) {
            if (WINZIP_AES != method)
                throw new ZipException(name
                        + " (encrypted compression method " + method + " is not supported)");
            final WinZipAesEntryReadOnlyFile
                    erof = new WinZipAesEntryReadOnlyFile(rof,
                        new WinZipAesEntryParameters(
                                parameters(
                                    WinZipAesParameters.class,
                                    getCryptoParameters()),
                                entry));
            // Authenticate and disable redundant CRC-32 check.
            erof.authenticate();
            check = false;
            final WinZipAesEntryExtraField field
                    = (WinZipAesEntryExtraField) entry.getExtraField(WINZIP_AES_ID);
            method = field.getMethod();
        }
        if (check) {
            // Check CRC-32 in the Local File Header or Data Descriptor.
            final long localCrc;
            if (entry.getGeneralPurposeBitFlag(GPBF_DATA_DESCRIPTOR)) {
                // The CRC-32 is in the Data Descriptor after the compressed
                // size.
                // Note the Data Descriptor's Signature is optional:
                // All newer apps should write it (and so does TrueZIP),
                // but older apps might not.
                final byte[] dd = new byte[8];
                archive.seek(offset + entry.getCompressedSize());
                archive.readFully(dd);
                final long ddSig = readUInt(dd, 0);
                localCrc = ddSig == DD_SIG
                        ? readUInt(dd, 4)
                        : ddSig;
            } else {
                // The CRC-32 in the Local File Header.
                localCrc = readUInt(lfh, 14);
            }
            if (entry.getCrc() != localCrc)
                throw new CRC32Exception(name, entry.getCrc(), localCrc);
        }
        InputStream in;
        final int bufSize = getBufferSize(entry);
        switch (method) {
            case DEFLATED:
                in = new PooledInflaterInputStream(
                        new DummyByteInputStream(rof), bufSize);
                break;
            case STORED:
                in = new ReadOnlyFileInputStream(rof);
                break;
            default:
                throw new ZipException(name
                    + " (compression method " + method + " is not supported)");
        }
        if (check)
            in = new CheckedInputStream(in, entry, bufSize);
        return in;
    }

    @SuppressWarnings("unchecked")
    private static <P extends ZipCryptoParameters> P parameters(
            final Class<P> type,
            final @CheckForNull ZipCryptoParameters param)
    throws ZipCryptoParametersException {
        // Order is important here to support multiple interface implementations!
        if (null == param) {
            throw new ZipCryptoParametersException("No crypto parameters available!");
        } else if (type.isAssignableFrom(param.getClass())) {
            return (P) param;
        } else if (param instanceof ZipCryptoParametersProvider) {
            return parameters(type,
                    ((ZipCryptoParametersProvider) param).get(type));
        } else {
            throw new ZipCryptoParametersException();
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
    final void assertOpen() throws ZipException {
        if (null == archive)
            throw new ZipException("ZIP file closed!");
    }

    /** An input stream which uses a pooled inflater. */
    private static final class PooledInflaterInputStream
    extends InflaterInputStream {
        private boolean closed;

        PooledInflaterInputStream(DummyByteInputStream in, int size) {
            super(in, Inflaters.allocate(), size);
        }

        @Override
        public void close() throws IOException {
            if (closed)
                return;
            closed = true;
            try {
                super.close();
            } finally {
                Inflaters.release(inf);
            }
        }
    } // PooledInflaterInputStream

    /**
     * Compares the actual CRC to the expected CRC in the close() method and
     * throws a CRC32Exception on a mismatch.
     */
    private static final class CheckedInputStream
    extends java.util.zip.CheckedInputStream {
        private final ZipEntry entry;
        private final int size;

        CheckedInputStream(
                final InputStream in,
                final ZipEntry entry,
                final int size) {
            super(in, new CRC32());
            this.entry = entry;
            this.size = size;
        }

        @Override
        public long skip(long toSkip) throws IOException {
            return skipWithBuffer(this, toSkip, new byte[size]);
        }

        @Override
        public void close() throws IOException {
            try {
                while (skip(Long.MAX_VALUE) > 0) { // process CRC-32 until EOF
                }
            } finally {
                super.close();
            }
            final long expected = entry.getCrc();
            final long computed = getChecksum().getValue();
            if (expected != computed)
                throw new CRC32Exception(entry.getName(), expected, computed);
        }
    } // CheckedInputStream

    /**
     * This method skips {@code toSkip} bytes in the given input stream
     * using the given buffer unless EOF or IOException.
     */
    private static long skipWithBuffer(
            final InputStream in,
            final long toSkip,
            final byte[] buf)
    throws IOException {
        long total = 0;
        for (long len; (len = toSkip - total) > 0; total += len) {
            len = in.read(buf, 0, len < buf.length ? (int) len : buf.length);
            if (len < 0)
                break;
        }
        return total;
    }

    /**
     * Closes the file.
     * This closes any allocate input streams reading from this ZIP file.
     *
     * @throws IOException if an error occurs closing the file.
     */
    @Override
    public void close() throws IOException {
        final ReadOnlyFile archive = this.archive;
        if (null == archive)
            return;
        this.archive = null;
        archive.close();
    }

    /**
     * A read only file input stream which adds a dummy zero byte to the end of
     * the input in order to support {@link PooledInflaterInputStream}.
     */
    private static final class DummyByteInputStream
    extends ReadOnlyFileInputStream {
        boolean added;

        DummyByteInputStream(ReadOnlyFile rof) {
            super(rof);
        }

        @Override
        public int read() throws IOException {
            final int read = rof.read();
            if (read < 0 && !added) {
                added = true;
                return 0;
            }
            return read;
        }

        @Override
        public int read(final byte[] buf, final int off, int len)
        throws IOException {
            if (0 == len)
                return 0;
            final int read = rof.read(buf, off, len);
            if (read < len && !added) {
                added = true;
                if (read < 0) {
                    buf[0] = 0;
                    return 1;
                } else {
                    buf[read] = 0;
                    return read + 1;
                }
            }
            return read;
        }

        /**
         * @return The number of bytes remaining in this entry, yet maximum
         *         {@code Integer.MAX_VALUE}.
         *         Note that this is only relevant for entries which have been
         *         stored with the {@code STORED} method.
         *         For entries stored according to the {@code DEFLATED}
         *         method, the value returned by this method on the
         *         {@code InputStream} returned by {@link #getInputStream}
         *         is actually determined by an {@link InflaterInputStream}.
         */
        @Override
        public int available() throws IOException {
            int available = super.available();
            return added || available >= Integer.MAX_VALUE
                    ? available
                    : available + 1;
        }
    } // DummyByteInputStream

    /**
     * An interval read only file which accounts for itself until it gets
     * closed.
     * Note that when an object of this class gets closed, the decorated read
     * only file, i.e. the raw zip file does NOT get closed!
     */
    private final class EntryReadOnlyFile extends IntervalReadOnlyFile {
        private boolean closed;

        EntryReadOnlyFile(long start, long length)
        throws IOException {
            super(RawZipFile.this.archive, start, length);
            assert null != RawZipFile.this.archive;
            RawZipFile.this.openEntries++;
        }

        @Override
        public void close() throws IOException {
            if (this.closed)
                return;
            this.closed = true;
            RawZipFile.this.openEntries--;
            // Never close the raw ZIP file!
            //super.close();
        }
    } // SegmentReadOnlyFile

    /** Maps a given offset to a file pointer position. */
    @SuppressWarnings("PackageVisibleInnerClass")
    static class OffsetMapper {
        long location(long offset) {
            return offset;
        }
    } // OffsetMapper

    /** Adds a start value to the given offset. */
    private static final class IrregularOffsetMapper extends OffsetMapper {
        final long start;

        IrregularOffsetMapper(long start) {
            this.start = start;
        }

        @Override
        long location(long offset) {
            return start + offset;
        }
    } // IrregularOffsetMapper

    /** A pool with a single read only file provided to its constructor. */
    private static final class SingleReadOnlyFilePool
    implements Pool<ReadOnlyFile, IOException> {
        final ReadOnlyFile rof;

        SingleReadOnlyFilePool(ReadOnlyFile rof) {
            this.rof = rof;
        }

        @Override
        public ReadOnlyFile allocate() {
            return rof;
        }

        @Override
        public void release(ReadOnlyFile rof) {
            assert this.rof == rof;
        }
    } // SingleReadOnlyFilePool
}
