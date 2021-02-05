/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.commons.zip;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.TreeMap;

import static global.namespace.truevfs.commons.zip.Constants.EMPTY;
import static global.namespace.truevfs.commons.zip.LittleEndian.readUShort;
import static global.namespace.truevfs.commons.zip.LittleEndian.writeShort;

/**
 * Represents a collection of {@link ExtraField extra fields} as they may
 * be present at several locations in ZIP files.
 *
 * @author  Christian Schlichtherle
 */
final class ExtraFields implements Cloneable {

    /**
     * The map of extra fields.
     * Maps from Header ID [{@link Integer}] to extra field [{@link ExtraField}].
     * Must not be {@code null}, but may be empty if no extra fields are used.
     * The map is sorted by Header IDs in ascending order.
     */
    private Map<Integer, ExtraField> extra = new TreeMap<>();

    /** Returns a shallow clone of this collection. */
    @Override
    @SuppressWarnings("AccessingNonPublicFieldOfAnotherObject")
    public ExtraFields clone() {
        try {
            final ExtraFields clone = (ExtraFields) super.clone();
            clone.extra = new TreeMap<>(extra);
            return clone;
        } catch (CloneNotSupportedException cannotHappen) {
            throw new AssertionError(cannotHappen);
        }
    }

    /** Returns the number of extra fields in this collection. */
    int size() { return extra.size(); }

    /**
     * Returns the extra field with the given Header ID or {@code null}
     * if no such extra field exists.
     *
     * @param headerId The requested Header ID.
     * @return The extra field with the given Header ID or {@code null}
     *         if no such extra field exists.
     * @throws IllegalArgumentException If {@code headerID} is not in
     *         the range of {@code 0} to {@link UShort#MAX_VALUE}
     *         ({@value net.truevfs.driver.zip.io.UShort#MAX_VALUE}).
     */
    @CheckForNull ExtraField get(final int headerId) {
        assert UShort.check(headerId);
        final ExtraField ef = extra.get(headerId);
        assert null == ef || ef.getHeaderId() == headerId;
        return ef;
    }

    /**
     * Stores the given extra field in this collection.
     *
     * @param ef The extra field to store in this collection.
     * @return The extra field previously associated with the Header ID of
     *         of the given extra field or {@code null} if no such
     *         extra field existed.
     * @throws NullPointerException If {@code ef} is {@code null}.
     * @throws IllegalArgumentException If the Header ID of the given Extra
     *         Field is not in the range of {@code 0} to
     *         {@link UShort#MAX_VALUE}
     *         ({@value net.truevfs.driver.zip.io.UShort#MAX_VALUE}).
     */
    ExtraField add(final ExtraField ef) {
        final int headerId = ef.getHeaderId();
        assert UShort.check(headerId);
        return extra.put(headerId, ef);
    }

    /**
     * Removes the extra field with the given Header ID.
     *
     * @param headerId The requested Header ID.
     * @return The extra field with the given Header ID or {@code null}
     *         if no such extra field exists.
     * @throws IllegalArgumentException If {@code headerID} is not in
     *         the range of {@code 0} to {@link UShort#MAX_VALUE}
     *         ({@value net.truevfs.driver.zip.io.UShort#MAX_VALUE}).
     */
    @Nullable ExtraField remove(final int headerId) {
        assert UShort.check(headerId);
        final ExtraField ef = extra.remove(headerId);
        assert null == ef || ef.getHeaderId() == headerId;
        return ef;
    }

    /**
     * Returns the number of bytes required to hold the extra fields.
     *
     * @return The length of the extra fields in bytes.
     *         May be {@code 0}.
     * @see #getDataBlock
     */
    int getDataSize() {
        final Map<Integer, ExtraField> extra = this.extra;
        if (extra.isEmpty()) return 0;
        int l = 0;
        for (ExtraField ef : extra.values()) l += 4 + ef.getDataSize();
        return l;
    }

    /**
     * Returns a protective copy of the extra fields.
     * {@code null} is never returned.
     *
     * @see #getDataSize
     */
    byte[] getDataBlock() {
        final int size = getDataSize();
        assert UShort.check(size);
        if (0 == size) return EMPTY;
        final byte[] data = new byte[size];
        writeTo(data, 0);
        return data;
    }

    /**
     * Deserializes this collection of extra fields from
     * the data block starting at the zero based offset {@code off} with
     * {@code len} bytes length in the byte array {@code buf}.
     * After return, this collection does not access {@code buf} anymore
     * and {@link #getDataSize} equals {@code len}.
     *
     * @param  buf The byte array to read the data block from.
     * @param  off The zero based offset in the byte array where the first byte
     *         of the data block is read from.
     * @param  len The length of the data block in bytes.
     * @throws IndexOutOfBoundsException If the byte array
     *         {@code buf} does not hold at least {@code len}
     *         bytes at the zero based offset {@code off}
     *         or if {@code len} is smaller than the extra field data requires.
     * @throws IllegalArgumentException If the data block does not conform to
     *         the ZIP File Format Specification.
     * @see    #getDataSize
     */
    void readFrom(final byte[] buf, int off, final int len)
    throws IndexOutOfBoundsException, IllegalArgumentException {
        assert UShort.check(len);
        final Map<Integer, ExtraField> map = new TreeMap<>();
        if (null != buf && 0 < len) {
            final int end = off + len;
            while (off < end) {
                final int headerId = readUShort(buf, off);
                off += 2;
                final int dataSize = readUShort(buf, off);
                off += 2;
                final ExtraField ef = ExtraField.create(headerId);
                ef.readFrom(buf, off, dataSize);
                off += dataSize;
                map.put(headerId, ef);
            }
            assert off == end;
        }
        extra = map;
    }

    /**
     * Serializes this collection of extra fields to
     * the data block starting at the zero based offset {@code off} with
     * {@link #getDataSize} bytes length in the byte array {@code buf}.
     * Upon return, this collection shall not access {@code data}
     * subsequently.
     *
     * @param  buf The byte array to write the data block to.
     * @param  off The zero based offset in the byte array where the first byte
     *         of the data block is written to.
     * @throws IndexOutOfBoundsException If the byte array
     *         {@code buf} does not hold at least {@link #getDataSize}
     *         bytes at the zero based offset {@code off}.
     * @see    #getDataSize
     */
    void writeTo(final byte[] buf, int off)
    throws IndexOutOfBoundsException {
       for (final ExtraField ef : extra.values()) {
            writeShort(ef.getHeaderId(), buf, off);
            off += 2;
            writeShort(ef.getDataSize(), buf, off);
            off += 2;
            ef.writeTo(buf, off);
            off += ef.getDataSize();
        }
    }
}