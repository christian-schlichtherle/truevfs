/*
 * Copyright (C) 2005-2011 Schlichtherle IT Services
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

import static de.schlichtherle.truezip.zip.Constants.*;
import static de.schlichtherle.truezip.zip.ExtraField.*;
import static de.schlichtherle.truezip.zip.LittleEndian.*;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import net.jcip.annotations.NotThreadSafe;

/**
 * Drop-in replacement for {@link java.util.zip.ZipEntry java.util.zip.ZipEntry}.
 * For every numeric property of this class, the default value is
 * {@code UNKNOWN} in order to indicate an unknown state and it's
 * permitted to set this value explicitly in order to reset the property.
 * <p>
 * Note that a {@code ZipEntry} object can be used with only one
 * {@link ZipFile} or {@link ZipOutputStream} instance.
 * Reusing the same {@code ZipEntry} object with a second object of these
 * classes is an error and may result in unpredictable behaviour.
 * <p>
 * In general, this class is <em>not</em> thread-safe.
 * However, it is safe to call only the getters of this class from multiple
 * threads concurrently.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
@DefaultAnnotation(NonNull.class)
public class ZipEntry implements Cloneable {

    // Bit masks for initialized fields.
    private static final int PLATFORM = 1, METHOD = 1 << 1,
                             CRC = 1 << 2, CSIZE = 1 << 3,
                             SIZE = 1 << 4, OFFSET = 1 << 5,
                             DTIME = 1 << 6;

    /** The unknown value for numeric properties. */
    public static final byte UNKNOWN = -1;

    /** Windows platform. */
    public static final short PLATFORM_FAT  = 0;

    /** Unix platform. */
    public static final short PLATFORM_UNIX = 3;

    /** Compression method for uncompressed (<em>stored</em>) entries. */
    public static final int STORED = 0;

    /** Compression method for compressed (<em>deflated</em>) entries. */
    public static final int DEFLATED = 8;

    /**
     * Pseudo compression method for WinZip AES encrypted entries.
     * 
     * @since TrueZIP 7.3
     */
    static final int WINZIP_AES = 99;

    /** General Purpose Bit Flag mask for encrypted data. */
    static int GPBF_ENCRYPTED = 1;

    static int GPBF_DATA_DESCRIPTOR = 1 << 3;
    static int GPBF_UTF8 = 1 << 11;

    /**
     * Smallest supported DOS date/time value in a ZIP file,
     * which is January 1<sup>st</sup>, 1980 AD 00:00:00 local time.
     */
    public static final long MIN_DOS_TIME = DateTimeConverter.MIN_DOS_TIME;

    /**
     * Largest supported DOS date/time value in a ZIP file,
     * which is December 31<sup>st</sup>, 2107 AD 23:59:58 local time.
     */
    public static final long MAX_DOS_TIME = DateTimeConverter.MAX_DOS_TIME;

    private byte init;                  // bit flags for init state
    private String name;
    private byte platform;              // 1 byte unsigned int (UByte)
    private short general;              // 2 bytes unsigned int (UShort)
    private short method;               // 2 bytes unsigned int (UShort)
    private int dtime;                  // 4 bytes unsigned int (UInt)
    private int crc;                    // 4 bytes unsigned int (UInt)
    private long csize;                 // 63 bits unsigned integer (ULong)
    private long size;                  // 63 bits unsigned integer (Ulong)

    /** Relative Offset Of Local File Header. */
    private long offset;                // 63 bits unsigned integer (ULong)

    /**
     * The map of Extra Fields.
     * Maps from Header ID [Integer] to Extra Field [ExtraField].
     * Should be {@code null} or may be empty if no Extra Fields are used.
     */
    private @CheckForNull ExtraFields fields;

    /** Comment field. */
    private @CheckForNull String comment;

    /** Constructs a new ZIP entry with the given name. */
    public ZipEntry(final String name) {
        UShort.check(name.length());
        this.name = name;
    }

    /**
     * Constructs a new ZIP entry with the given name and all other properties
     * copied from the given template.
     */
    public ZipEntry(final String name, final ZipEntry template) {
        UShort.check(name.length());
        this.init = template.init;
        this.name = name;
        this.platform = template.platform;
        this.general = template.general;
        this.method = template.method;
        this.dtime = template.dtime;
        this.crc = template.crc;
        this.csize = template.csize;
        this.size = template.size;
        this.offset = template.offset;
        final ExtraFields templateFields = template.fields;
        this.fields = templateFields == null ? null : templateFields.clone();
        this.comment = template.comment;
    }

    @Override
    public ZipEntry clone() {
        final ZipEntry entry;
        try {
            entry = (ZipEntry) super.clone();
        } catch (CloneNotSupportedException ex) {
            throw new AssertionError(ex);
        }
        final ExtraFields fields = this.fields;
        entry.fields = fields == null ? null : fields.clone();
        return entry;
    }

    private boolean isInit(final int mask) {
        return 0 != (init & mask);
    }

    private void setInit(final int mask, final boolean init) {
        if (init)
            this.init |=  mask;
        else
            this.init &= ~mask;
    }

    /** Returns the ZIP entry name. */
    public final String getName() {
        return name;
    }

    /**
     * Returns true if and only if this ZIP entry represents a directory entry
     * (i.e. end with {@code '/'}).
     */
    public final boolean isDirectory() {
        return name.endsWith("/");
    }

    public final short getPlatform() {
        return isInit(PLATFORM) ? (short) (platform & UByte.MAX_VALUE) : UNKNOWN;
    }

    public final void setPlatform(final short platform) {
        final boolean known = UNKNOWN != platform;
        if (known) {
            UByte.check(platform, name, "Platform out of range");
            this.platform = (byte) platform;
        } else {
            this.platform = 0;
        }
        setInit(PLATFORM, known);
    }

    final short getEncodedPlatform() {
        return (short) (platform & UByte.MAX_VALUE);
    }

    final void setEncodedPlatform(final int platform) {
        assert UByte.check(platform);
        this.platform = (byte) platform;
        setInit(PLATFORM, true);
    }

    /** Returns the General Purpose Bit Flags. */
    final int getGeneralPurposeBitFlags() {
        return general & UShort.MAX_VALUE;
    }

    /** Sets the General Purpose Bit Flags. */
    final void setGeneralPurposeBitFlags(final int general) {
        assert UShort.check(general);
        this.general = (short) general;
    }

    /** Returns the indexed General Purpose Bit Flag. */
    final boolean getGeneralPurposeBitFlag(final int mask) {
        return 0 != (general & mask);
    }

    /** Sets the indexed General Purpose Bit Flag. */
    final void setGeneralPurposeBitFlag(final int mask, final boolean bit) {
        if (bit)
            general |=  mask;
        else
            general &= ~mask;
    }

    /**
     * Returns {@code true} if and only if this ZIP entry is encrypted.
     * Note that only WinZip AES encryption is currently supported.
     * 
     * @return {@code true} if and only if this ZIP entry is encrypted.
     * @since   TrueZIP 7.3
     */
    public final boolean isEncrypted() {
        return getGeneralPurposeBitFlag(GPBF_ENCRYPTED);
    }

    /**
     * Sets the encryption flag for this ZIP entry.
     * Note that only WinZip AES encryption is currently supported.
     * 
     * @param encrypted whether or not this ZIP entry should get encrypted.
     * @since   TrueZIP 7.3
     */
    public final void setEncrypted(boolean encrypted) {
        setGeneralPurposeBitFlag(GPBF_ENCRYPTED, encrypted);
    }

    /**
     * Returns the compression method for this entry.
     *
     * @see #setMethod
     * @see ZipOutputStream#getMethod
     */
    public final int getMethod() {
        return isInit(METHOD) ? method & UShort.MAX_VALUE : UNKNOWN;
    }

    /**
     * Sets the compression method for this entry.
     *
     * @see #getMethod
     * @see ZipOutputStream#setMethod
     * @throws IllegalArgumentException If {@code method} is not
     *         {@link #STORED}, {@link #DEFLATED} or {@link #UNKNOWN}.
     */
    public final void setMethod(final int method) {
        switch (method) {
            case STORED:
            case DEFLATED:
            case WINZIP_AES:
                this.method = (short) method;
                setInit(METHOD, true);
                break;
            case UNKNOWN:
                this.method = 0;
                setInit(METHOD, false);
                break;
            default:
                throw new IllegalArgumentException(
                        name + " (unsupported compression method " + method + ")");
        }
    }

    final int getEncodedMethod() {
        return method & UShort.MAX_VALUE;
    }

    final void setEncodedMethod(final int method) {
        assert UShort.check(method);
        this.method = (short) method;
        setInit(METHOD, true);
    }

    public final long getTime() {
        if (!isInit(DTIME))
            return UNKNOWN;
        return getDateTimeConverter().toJavaTime(dtime & UInt.MAX_VALUE);
    }

    public final void setTime(final long jtime) {
        final boolean known = UNKNOWN != jtime;
        if (known) {
            this.dtime = (int) getDateTimeConverter().toDosTime(jtime);
        } else {
            this.dtime = 0;
        }
        setInit(DTIME, known);
    }

    final long getEncodedTime() {
        return dtime & UInt.MAX_VALUE;
    }

    final void setEncodedTime(final long dtime) {
        assert UInt.check(dtime);
        this.dtime = (int) dtime;
        setInit(DTIME, true);
    }

    /**
     * Returns a {@link DateTimeConverter} for the conversion of Java time
     * to DOS date/time fields and vice versa.
     * <p>
     * The implementation in the class {@link ZipEntry} returns
     * {@link DateTimeConverter#JAR}.
     *
     * @return A {@link DateTimeConverter} - never {@code null}.
     * @see DateTimeConverter
     */
    protected DateTimeConverter getDateTimeConverter() {
        return DateTimeConverter.JAR;
    }

    public final long getCrc() {
        return isInit(CRC) ? crc & UInt.MAX_VALUE : UNKNOWN;
    }

    public final void setCrc(final long crc) {
        final boolean known = UNKNOWN != crc;
        if (known) {
            UInt.check(crc, name, "CRC-32 out of range");
            this.crc = (int) crc;
        } else {
            this.crc = 0;
        }
        setInit(CRC, known);
    }

    final long getEncodedCrc() {
        return crc & UInt.MAX_VALUE;
    }

    final void setEncodedCrc(final long crc) {
        assert UInt.check(crc);
        this.crc = (int) crc;
        setInit(CRC, true);
    }

    /**
     * Returns the compressed size of this entry.
     *
     * @see #setCompressedSize
     */
    public final long getCompressedSize() {
        return isInit(CSIZE) ? csize : UNKNOWN;
    }

    /**
     * Sets the compressed size of this entry.
     *
     * @param csize The Compressed Size.
     * @throws IllegalArgumentException If {@code csize} is not in the
     *         range from {@code 0} to {@link ULong#MAX_VALUE}
     *         ({@value de.schlichtherle.truezip.zip.ULong#MAX_VALUE}).
     * @see #getCompressedSize
     */
    public final void setCompressedSize(final long csize) {
        final boolean known = UNKNOWN != csize;
        if (known) {
            ULong.check(csize, name, "Compressed Size out of range");
            this.csize = csize;
        } else {
            this.csize = 0;
        }
        setInit(CSIZE, known);
    }

    final long getEncodedCompressedSize() {
        return UInt.MAX_VALUE <= csize || FORCE_ZIP64_EXT ? UInt.MAX_VALUE : csize;
    }

    final void setEncodedCompressedSize(final long csize) {
        assert ULong.check(csize);
        this.csize = csize;
        setInit(CSIZE, true);
    }

    /**
     * Returns the uncompressed size of this entry.
     *
     * @see #setCompressedSize
     */
    public final long getSize() {
        return isInit(SIZE) ? size : UNKNOWN;
    }

    /**
     * Sets the uncompressed size of this entry.
     *
     * @param size The (Uncompressed) Size.
     * @throws IllegalArgumentException If {@code size} is not in the
     *         range from {@code 0} to {@link ULong#MAX_VALUE}
     *         ({@value de.schlichtherle.truezip.zip.ULong#MAX_VALUE}).
     * @see #getCompressedSize
     */
    public final void setSize(final long size) {
        final boolean known = UNKNOWN != size;
        if (known) {
            ULong.check(size, name, "Uncompressed Size out of range");
            this.size = size;
        } else {
            this.size = 0;
        }
        setInit(SIZE, known);
    }

    final long getEncodedSize() {
        return UInt.MAX_VALUE <= size || FORCE_ZIP64_EXT ? UInt.MAX_VALUE : size;
    }

    final void setEncodedSize(final long size) {
        assert ULong.check(size);
        this.size = size;
        setInit(SIZE, true);
    }

    final long getOffset() {
        return isInit(OFFSET) ? offset : UNKNOWN;
    }

    final long getEncodedOffset() {
        return UInt.MAX_VALUE <= offset || FORCE_ZIP64_EXT ? UInt.MAX_VALUE : offset;
    }

    final void setEncodedOffset(final long offset) {
        assert ULong.check(offset);
        this.offset = offset;
        setInit(OFFSET, true);
    }

    final @Nullable ExtraField getExtraField(int headerId) {
        final ExtraFields fields = this.fields;
        return fields == null ? null : fields.get(headerId);
    }

    final @Nullable ExtraField addExtraField(final ExtraField field) {
        assert null != field;
        ExtraFields fields = this.fields;
        if (null == fields)
            this.fields = fields = new ExtraFields();
        return fields.add(field);
    }

    final @Nullable ExtraField removeExtraField(final int headerId) {
        final ExtraFields fields = this.fields;
        return null != fields ? fields.remove(headerId) : null;
    }

    /**
     * Returns a protective copy of the serialized Extra Fields.
     * Note that unlike its template {@link java.util.zip.ZipEntry#getExtra()},
     * this method never returns {@code null}.
     *
     * @return A new byte array holding the serialized Extra Fields.
     *         {@code null} is never returned.
     */
    public final byte[] getExtra() {
        return getExtraFields(false);
    }

    /**
     * Sets the serialized Extra Fields by making a protective copy.
     *
     * @param data The byte array holding the serialized Extra Fields.
     */
    public final void setExtra(final @CheckForNull byte[] data) {
        if (null != data)
            UShort.check(data.length, "Extra Fields too large", null);
        if (null == data || data.length <= 0) {
            this.fields = null;
        } else {
            setExtraFields(data, false);
        }
    }

    /**
     * Returns a protective copy of the serialized Extra Fields.
     *
     * @param  zip64 Whether or not a ZIP64 Extended Information Extra Field,
     *         if present, shall be included in the returned data or not.
     * @return A new byte array holding the serialized Extra Fields.
     *         {@code null} is never returned.
     * @see    #getEncodedExtraFields()
     */
    final byte[] getEncodedExtraFields() {
        return getExtraFields(true);
    }

    /**
     * Set extra fields and parse ZIP64 extra field.
     */
    final void setEncodedExtraFields(final byte[] data) {
        assert 0 < data.length && UShort.check(data.length);
        setExtraFields(data, true);
    }

    private byte[] getExtraFields(final boolean zip64) {
        ExtraFields fields = this.fields;
        if (zip64) {
            final ExtraField field = composeZip64ExtraField();
            if (null != field) {
                fields = null != fields ? fields.clone() : new ExtraFields();
                fields.add(field);
            }
        } else {
            assert null == fields || null == fields.get(ZIP64_HEADER_ID);
        }
        return null == fields ? EMPTY : fields.getExtra();
    }

    private void setExtraFields(final byte[] data, final boolean zip64) {
        ExtraFields fields = this.fields;
        if (null == fields)
            this.fields = fields = new ExtraFields();
        fields.readFrom(data, 0, data.length);
        if (zip64)
            parseZip64ExtraField();
        assert fields == this.fields;
        fields.remove(ZIP64_HEADER_ID);
        if (fields.size() <= 0) {
            assert 0 == fields.size();
            this.fields = null;
        }
    }

    /**
     * Composes a ZIP64 Extended Information Extra Field from the properties
     * of this entry.
     * If no ZIP64 Extended Information Extra Field is required it is removed
     * from the collection of Extra Fields.
     */
    private @CheckForNull ExtraField composeZip64ExtraField() {
        final byte[] data = new byte[3 * 8]; // maximum size
        int off = 0;
        // Write out Uncompressed Size.
        final long size = getSize();
        if (UInt.MAX_VALUE <= size || FORCE_ZIP64_EXT && UNKNOWN != size) {
            writeLong(size, data, off);
            off += 8;
        }
        // Write out Compressed Size.
        final long csize = getCompressedSize();
        if (UInt.MAX_VALUE <= csize || FORCE_ZIP64_EXT && UNKNOWN != csize) {
            writeLong(csize, data, off);
            off += 8;
        }
        // Write out Relative Header Offset.
        final long offset = getOffset();
        if (UInt.MAX_VALUE <= offset || FORCE_ZIP64_EXT && UNKNOWN != offset) {
            writeLong(offset, data, off);
            off += 8;
        }
        // Create ZIP64 Extended Information Extra Field from serialized data.
        final ExtraField field;
        if (off > 0) {
            field = new DefaultExtraField(ZIP64_HEADER_ID);
            field.readFrom(data, 0, off);
        } else {
            field = null;
        }
        return field;
    }

    /**
     * Parses the properties of this entry from the ZIP64 Extended Information
     * Extra Field, if present.
     * The ZIP64 Extended Information Extra Field is <em>not</em> removed.
     */
    private void parseZip64ExtraField() {
        final ExtraFields fields = this.fields;
        if (null == fields)
            return;
        final ExtraField ef = fields.get(ZIP64_HEADER_ID);
        if (null == ef)
            return;
        final byte[] data = ef.getDataBlock();
        int off = 0;
        // Read in Uncompressed Size.
        final long size = getEncodedSize();
        if (size >= UInt.MAX_VALUE) {
            assert UInt.MAX_VALUE == size;
            setEncodedSize(readLong(data, off));
            off += 8;
        }
        // Read in Compressed Size.
        final long csize = getEncodedCompressedSize();
        if (csize >= UInt.MAX_VALUE) {
            assert UInt.MAX_VALUE == csize;
            setEncodedCompressedSize(readLong(data, off));
            off += 8;
        }
        // Read in Relative Header Offset.
        final long offset = getEncodedOffset();
        if (offset >= UInt.MAX_VALUE) {
            assert UInt.MAX_VALUE == offset;
            setEncodedOffset(readLong(data, off));
            //off += 8;
        }
    }

    public final @CheckForNull String getComment() {
        return comment;
    }

    public final void setComment(final @CheckForNull String comment) {
        if (null != comment)
            UShort.check(comment.length(), name, "Comment too long");
        this.comment = comment;
    }

    final String getDecodedComment() {
        final String comment = this.comment;
        return null != comment ? comment : "";
    }

    final void setDecodedComment(final String comment) {
        assert UShort.check(comment.length());
        this.comment = comment;
    }

    final boolean isDataDescriptorRequired() {
        return UNKNOWN == getCrc()
                || UNKNOWN == getCompressedSize()
                || UNKNOWN == getSize();
    }

    final boolean isZip64ExtensionsRequired() {
        // Offset MUST be considered in decision about ZIP64 format - see
        // description of Data Descriptor in ZIP File Format Specification!
        if (FORCE_ZIP64_EXT)
            return true /*UNKNOWN != getCompressedSize()
                    || UNKNOWN != getSize()
                    || UNKNOWN != getOffset()*/;
        else
            return UInt.MAX_VALUE <= getCompressedSize()
                    || UInt.MAX_VALUE <= getSize()
                    || UInt.MAX_VALUE <= getOffset();
    }

    /** Returns a string representation of this object. */
    @Override
    public String toString() {
        return new StringBuilder(getClass().getName())
                .append("[name=")
                .append(getName())
                .append(",time=")
                .append(getTime())
                .append(",method=")
                .append(getMethod())
                .append(",size=")
                .append(getSize())
                .append(",compressedSize=")
                .append(getCompressedSize())
                .append(",crc=")
                .append(getCrc())
                .append("]")
                .toString();
    }
}
