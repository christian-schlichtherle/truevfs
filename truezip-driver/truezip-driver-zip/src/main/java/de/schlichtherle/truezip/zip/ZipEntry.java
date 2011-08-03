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

import static de.schlichtherle.truezip.zip.ZipConstants.*;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.nio.charset.Charset;
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

    // Bit indices for initialized fields.
    private static final int    PLATFORM = 0, GENERAL  = 1,
                                METHOD   = 2, CRC      = 3;

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

    /** Pseudo Compression method for WinZip AES encrypted entries. */
    static final int ENCRYPTED = 99;

    /** General Purpose Bit Flag for encrypted data. */
    static int GPBF_ENCRYPTED = 0;

    static int GPBF_DATA_DESCRIPTOR = 3;
    static int GPBF_UTF8 = 11;

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

    private byte init;                  // bit flag for init state
    private String name;
    private byte platform = UNKNOWN;    // 1 byte unsigned int (UByte)
    private short general = 0;          // 2 bytes unsigned int (UShort)
    private short method = UNKNOWN;     // 2 bytes unsigned int (UShort)
    private long jTime = UNKNOWN;       // Java time (!)
    private int crc = UNKNOWN;          // 4 bytes unsigned int (ULong)
    private long csize = UNKNOWN;       // 63 bits unsigned integer (ULong)
    private long size = UNKNOWN;        // 63 bits unsigned integer (Ulong)

    /** Relative Offset Of Local File Header. */
    private long offset = UNKNOWN;      // 63 bits unsigned integer (ULong)

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
        this.init = template.init;
        this.name = name;
        this.platform = template.platform;
        this.general = template.general;
        this.method = template.method;
        this.jTime = template.jTime;
        this.crc = template.crc;
        this.csize = template.csize;
        this.size = template.size;
        this.offset = template.offset;
        setExtra0(template.getExtra());
        this.comment = template.comment;
    }

    @Override
    public ZipEntry clone() {
        final ZipEntry entry;
        try {
            entry = (ZipEntry) super.clone();
        } catch (CloneNotSupportedException cannotHappen) {
            throw new AssertionError(cannotHappen);
        }
        entry.setExtra(getExtra());
        return entry;
    }

    private boolean isInit(final int index) {
        assert 0 <= index && index < 8 : "Bit index out of range: " + index;
        return (init & (1 << index)) != 0;
    }

    private void setInit(final int index, final boolean init) {
        assert 0 <= index && index < 8 : "Bit index out of range: " + index;
        if (init)
            this.init |=   1 << index;
        else
            this.init &= ~(1 << index);
    }

    /** Returns the ZIP entry name. */
    public String getName() {
        return name;
    }

    final int getNameLength(final Charset charset) {
        return null == name ? 0 : charset.encode(name).limit();
    }

    /**
     * Returns true if and only if this ZIP entry represents a directory entry
     * (i.e. end with {@code '/'}).
     */
    public boolean isDirectory() {
        return name.endsWith("/");
    }

    public short getPlatform() {
        return isInit(PLATFORM) ? (short) (platform & 0xFF) : UNKNOWN;
    }

    public void setPlatform(final short platform) {
        final boolean known = platform != UNKNOWN;
        if (known)
            UByte.check(platform, name, "Platform out of range");
        setInit(PLATFORM, known);
        this.platform = (byte) platform;
    }

    /** Returns the General Purpose Bit Flags. */
    int getGeneral() {
        return isInit(GENERAL) ? general & UShort.MAX_VALUE : UNKNOWN;
    }

    /** Sets the General Purpose Bit Flags. */
    void setGeneral(final int general) {
        final boolean known = general != UNKNOWN;
        if (known)
            UShort.check(general, name, "General Purpose Bit Flag out of range");
        setInit(GENERAL, known);
        this.general = known ? (short) general : 0;
    }

    /** Returns the indexed General Purpose Bit. */
    final boolean getGeneralBit(final int index) {
        if (index < 0 || 15 < index)
            throw new IllegalArgumentException(name
            + ": General Purpose Bit Flag index out of range: " + index);
        return /*isInit(GENERAL) &&*/ (general & (1 << index)) != 0;
    }

    /** Sets the indexed General Purpose Bit. */
    final void setGeneralBit(final int index, final boolean bit) {
        if (index < 0 || 15 < index)
            throw new IllegalArgumentException(name
            + ": General Purpose Bit Flag index out of range: " + index);
        setInit(GENERAL, true);
        if (bit)
            general |=   1 << index;
        else
            general &= ~(1 << index);
    }

    /**
     * Returns the compression method for this entry.
     *
     * @see #setMethod
     * @see ZipOutputStream#getMethod
     */
    public int getMethod() {
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
    public void setMethod(final int method) {
        final boolean known = method != UNKNOWN;
	if (known && method != STORED && method != DEFLATED)
            throw new IllegalArgumentException(
                    name + ": unsupported Compression Method: " + method);
        setInit(METHOD, known);
        this.method = (short) method;
    }

    /**
     * Returns {@code true} if and only if this ZIP entry is encrypted.
     * Note that only WinZip AES encryption is currently supported.
     * 
     * @return {@code true} if and only if this ZIP entry is encrypted.
     */
    public boolean isEncrypted() {
        return getGeneralBit(GPBF_ENCRYPTED);
    }

    /**
     * Sets the encryption flag for this ZIP entry.
     * Note that only WinZip AES encryption is currently supported.
     * 
     * @param encrypted whether or not this ZIP entry should get encrypted.
     */
    public void setEncrypted(boolean encrypted) {
        setGeneralBit(GPBF_ENCRYPTED, encrypted);
    }
    
    protected long getDosTime() {
        return UNKNOWN == jTime
                ? UNKNOWN
                : getDateTimeConverter().toDosTime(jTime);
    }

    protected void setDosTime(final long dTime) {
        this.jTime = UNKNOWN == dTime
                ? UNKNOWN
                : getDateTimeConverter().toJavaTime(dTime);
    }

    public long getTime() {
        return jTime;
    }

    public void setTime(final long jTime) {
        if (UNKNOWN == jTime) {
            this.jTime = UNKNOWN;
        } else {
            // Adjust to lower resolution of DOS date/time.
            DateTimeConverter dtc = getDateTimeConverter();
            this.jTime = dtc.toJavaTime(dtc.toDosTime(jTime));
        }
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

    public long getCrc() {
        return isInit(CRC) ? crc & UInt.MAX_VALUE : UNKNOWN;
    }

    public void setCrc(final long crc) {
        final boolean known = crc != UNKNOWN;
        if (known)
            UInt.check(crc, name, "CRC-32 out of range");
        setInit(CRC, known);
        this.crc = (int) crc;
    }

    long getCompressedSize32() {
        if (csize == UNKNOWN)
            return UNKNOWN;
        return csize > UInt.MAX_VALUE || FORCE_ZIP64_EXT ? UInt.MAX_VALUE : csize;
    }

    void setCompressedSize32(final long csize) {
        if (csize != UNKNOWN)
            UInt.check(csize, name, "Compressed Size out of range");
        this.csize = csize;
    }

    /**
     * Returns the compressed size of this entry.
     *
     * @see #setCompressedSize
     */
    public long getCompressedSize() {
        return csize;
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
    public void setCompressedSize(final long csize) {
        setCompressedSize64(csize);
    }

    private void setCompressedSize64(final long csize) {
        if (csize != UNKNOWN)
            ULong.check(csize, name, "Compressed Size out of range");
        this.csize = csize;
    }

    long getSize32() {
        if (size == UNKNOWN)
            return UNKNOWN;
        return size > UInt.MAX_VALUE || FORCE_ZIP64_EXT ? UInt.MAX_VALUE : size;
    }

    void setSize32(final long size) {
        if (size != UNKNOWN)
            UInt.check(size, name, "Uncompressed Size out of range");
        this.size = size;
    }

    /**
     * Returns the uncompressed size of this entry.
     *
     * @see #setCompressedSize
     */
    public long getSize() {
        return size;
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
    public void setSize(final long size) {
        setSize64(size);
    }

    private void setSize64(final long size) {
        if (size != UNKNOWN)
            ULong.check(size, name, "Uncompressed Size out of range");
        this.size = size;
    }

    long getOffset32() {
        if (offset == UNKNOWN)
            return UNKNOWN;
        return offset > UInt.MAX_VALUE || FORCE_ZIP64_EXT ? UInt.MAX_VALUE : offset;
    }

    void setOffset32(final long offset) {
        if (offset != UNKNOWN)
            UInt.check(offset, name, "Relative Header Offset out of range");
        this.offset = offset;
    }

    long getOffset() {
        return offset;
    }

    void setOffset(final long offset) {
        setOffset64(offset);
    }

    private void setOffset64(final long offset) {
        if (offset != UNKNOWN)
            ULong.check(offset, name, "Relative Header Offset out of range");
        this.offset = offset;
    }

    /**
     * Returns a protective copy of the serialized Extra Fields.
     * Note that unlike its template {@link java.util.zip.ZipEntry#getExtra},
     * this method never returns {@code null}.
     *
     * @return A new byte array holding the serialized Extra Fields.
     *         {@code null} is never returned.
     */
    public byte[] getExtra() {
        return getExtra(true);
    }

    /**
     * Returns a protective copy of the serialized Extra Fields.
     *
     * @param zip64 Whether or not a ZIP64 Extended Information Extra Field,
     *        if present, shall be included in the return data or not.
     * @return A new byte array holding the serialized Extra Fields.
     *         {@code null} is never returned.
     * @see #getExtra()
     */
    byte[] getExtra(final boolean zip64) {
        final ExtraFields fields = getFields(zip64);
        return null == fields ? EMPTY : fields.getExtra();
    }

    private @CheckForNull ExtraFields getFields(final boolean zip64) {
        ExtraFields fields = this.fields;
        if (zip64) {
            final ExtraField field = compileZip64ExtraField();
            if (null != field) {
                fields = null != fields ? fields.clone() : new ExtraFields();
                fields.put(field);
            }
        } else if (null != fields) {
            ExtraField field = fields.get(ExtraField.ZIP64_HEADER_ID);
            if (null != field) {
                fields = fields.clone();
                field = fields.remove(ExtraField.ZIP64_HEADER_ID);
                assert ExtraField.ZIP64_HEADER_ID == field.getHeaderID();
            }
        }
        return fields;
    }

    /**
     * Returns the length of the serialized Extra Fields in bytes.
     *
     * @return The minimum number of bytes needed to hold the serialized
     *         Extra Fields.
     */
    final int getExtraLength() {
        return getExtra(true).length;
    }

    /**
     * Sets the serialized Extra Fields by making a protective copy.
     *
     * @param data The byte array holding the serialized Extra Fields.
     */
    public void setExtra(@CheckForNull byte[] data) {
        setExtra0(data);
    }

    private void setExtra0(final @CheckForNull byte[] data) {
        if (null == data || 0 >= data.length) {
            fields = null;
        } else {
            if (null == fields)
                fields = new ExtraFields();
            fields.readFrom(data, 0, data.length);
            parseZip64ExtraField();
            assert null != fields;
            fields.remove(ExtraField.ZIP64_HEADER_ID);
            if (0 >= fields.size()) {
                assert fields.size() == 0;
                fields = null;
            }
        }
    }

    /**
     * Parses the properties of this entry from the ZIP64 Extended Information
     * Extra Field, if present.
     * The ZIP64 Extended Information Extra Field is <em>not</em> removed.
     */
    private void parseZip64ExtraField() {
        if (null == fields)
            return;
        final ExtraField ef = fields.get(ExtraField.ZIP64_HEADER_ID);
        if (null == ef)
            return;
        final byte[] data = ef.getDataBlock();
        int off = 0;
        // Read in Uncompressed Size.
        final long size = getSize32();
        if (size >= UInt.MAX_VALUE) {
            assert size == UInt.MAX_VALUE;
            setSize64(LittleEndian.readLong(data, off));
            off += 8;
        }
        // Read in Compressed Size.
        final long csize = getCompressedSize32();
        if (csize >= UInt.MAX_VALUE) {
            assert csize == UInt.MAX_VALUE;
            setCompressedSize64(LittleEndian.readLong(data, off));
            off += 8;
        }
        // Read in Relative Header Offset.
        final long offset = getOffset32();
        if (offset >= UInt.MAX_VALUE) {
            assert offset == UInt.MAX_VALUE;
            setOffset64(LittleEndian.readLong(data, off));
            //off += 8;
        }
    }

    /**
     * Compiles a ZIP64 Extended Information Extra Field from the properties
     * of this entry.
     * If no ZIP64 Extended Information Extra Field is required it is removed
     * from the collection of Extra Fields.
     */
    private @CheckForNull ExtraField compileZip64ExtraField() {
        final byte[] data = new byte[3 * 8]; // maximum size
        int off = 0;
        // Write out Uncompressed Size.
        final long size = getSize();
        if (size >= UInt.MAX_VALUE || FORCE_ZIP64_EXT && size >= 0) {
            LittleEndian.writeLong(size, data, off);
            off += 8;
        }
        // Write out Compressed Size.
        final long csize = getCompressedSize();
        if (csize >= UInt.MAX_VALUE || FORCE_ZIP64_EXT && csize >= 0) {
            LittleEndian.writeLong(csize, data, off);
            off += 8;
        }
        // Write out Relative Header Offset.
        final long offset = getOffset();
        if (offset >= UInt.MAX_VALUE || FORCE_ZIP64_EXT && offset >= 0) {
            LittleEndian.writeLong(offset, data, off);
            off += 8;
        }
        // Create ZIP64 Extended Information Extra Field from serialized data.
        final ExtraField field;
        if (off > 0) {
            field = new DefaultExtraField(ExtraField.ZIP64_HEADER_ID);
            field.readFrom(data, 0, off);
        } else {
            field = null;
        }
        return field;
    }

    public @CheckForNull String getComment() {
        return comment;
    }

    final int getCommentLength(final Charset charset) {
        return null == comment ? 0 : charset.encode(comment).limit();
    }

    public void setComment(final @CheckForNull String comment) {
        if (null != comment)
            UShort.check(comment.length(), name, "Comment too long");
        this.comment = comment;
    }

    /** Returns a string representation of this object. */
    @Override
    public String toString() {
        return new StringBuilder(getClass().getName())
                .append("[name=")
                .append(getName())
                .append(",method=")
                .append(getMethod())
                .append(",crc=")
                .append(getCrc())
                .append(",compressedSize=")
                .append(getCompressedSize())
                .append(",size=")
                .append(getSize())
                .append(",time=")
                .append(getTime())
                .append("]")
                .toString();
    }
}
