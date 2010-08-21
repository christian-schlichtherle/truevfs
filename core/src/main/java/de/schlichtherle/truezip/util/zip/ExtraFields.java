/*
 * Copyright (C) 2007-2010 Schlichtherle IT Services
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

package de.schlichtherle.truezip.util.zip;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/**
 * Represents a collection of {@link ExtraField Extra Fields} as they may
 * be present at several locations in ZIP archive files.
 * <p>
 * This class is <em>not</em> thread-safe.
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 */
final class ExtraFields implements Cloneable {

    /**
     * The map of Extra Fields.
     * Maps from Header ID [{@link Integer}] to Extra Field [{@link ExtraField}].
     * Must not be {@code null}, but may be empty if no Extra Fields are used.
     * The map is sorted by Header IDs in ascending order.
     */
    private Map extra = new TreeMap();

    /** Returns a shallow clone of this collection. */
    @Override
    public Object clone() {
        try {
            final ExtraFields clone = (ExtraFields) super.clone();
            clone.extra = new TreeMap(extra);
            return clone;
        } catch (CloneNotSupportedException cannotHappen) {
            throw new AssertionError(cannotHappen);
        }
    }

    /** Returns the number of Extra Fields in this collection. */
    public int size() {
        return extra.size();
    }

    /**
     * Returns the Extra Field with the given Header ID or {@code null}
     * if no such Extra Field exists.
     * 
     * @param headerID The requested Header ID.
     * @return The Extra Field with the given Header ID or {@code null}
     *         if no such Extra Field exists.
     * @throws IllegalArgumentException If {@code headerID} is not in
     *         the range of {@code 0} to {@link UShort#MAX_VALUE}
     *         ({@value de.schlichtherle.truezip.util.zip.UShort#MAX_VALUE}).
     */
    public ExtraField get(final int headerID) {
        UShort.check(headerID);
        // TODO: For JSE 5: final ExtraField ef = (ExtraField) extra.get(Integer.valueOf(headerID));
        final ExtraField ef = (ExtraField) extra.get(new Integer(headerID));
        assert ef == null || headerID == ef.getHeaderID();
        return ef;
    }

    /**
     * Stores the given Extra Field in this collection.
     * 
     * @param ef The Extra Field to store in this collection.
     * @return The Extra Field previously associated with the Header ID of
     *         of the given Extra Field or {@code null} if no such
     *         Extra Field existed.
     * @throws NullPointerException If {@code ef} is {@code null}.
     * @throws IllegalArgumentException If the Header ID of the given Extra
     *         Field is not in the range of {@code 0} to
     *         {@link UShort#MAX_VALUE}
     *         ({@value de.schlichtherle.truezip.util.zip.UShort#MAX_VALUE}).
     */
    public ExtraField put(final ExtraField ef) {
        if (ef == null)
            throw new NullPointerException("ef");
        final int headerID = ef.getHeaderID();
        UShort.check(headerID);
        // TODO: For JSE 5: return (ExtraField) extra.put(Integer.valueOf(headerID), ef);
        return (ExtraField) extra.put(new Integer(headerID), ef);
    }

    /**
     * Removes the Extra Field with the given Header ID.
     * 
     * @param headerID The requested Header ID.
     * @return The Extra Field with the given Header ID or {@code null}
     *         if no such Extra Field exists.
     * @throws IllegalArgumentException If {@code headerID} is not in
     *         the range of {@code 0} to {@link UShort#MAX_VALUE}
     *         ({@value de.schlichtherle.truezip.util.zip.UShort#MAX_VALUE}).
     */
    public ExtraField remove(final int headerID) {
        UShort.check(headerID);
        // TODO: For JSE 5: final ExtraField ef = (ExtraField) extra.remove(Integer.valueOf(headerID));
        final ExtraField ef = (ExtraField) extra.remove(new Integer(headerID));
        assert ef == null || headerID == ef.getHeaderID();
        return ef;
    }

    /**
     * Returns the number of bytes required to hold the Extra Fields.
     * 
     * @return The length of the Extra Fields in bytes.
     *         May be {@code 0}.
     * @see #getExtra
     */
    int getExtraLength() {
        if (extra.isEmpty())
            return 0;

        int l = 0;
        for (final Iterator it = extra.values().iterator(); it.hasNext(); ) {
            final ExtraField ef = (ExtraField) it.next();
            l += 4 + ef.getDataSize();
        }
        return l;
    }

    /**
     * Returns a protective copy of the Extra Fields.
     * {@code null} is never returned.
     * 
     * @see #getExtraLength
     */
    byte[] getExtra() {
        final int size = getExtraLength();
        UShort.check(size);
        if (size == 0)
            return ZIP.EMPTY;

        final byte[] data = new byte[size];
        writeTo(data, 0);
        return data;
    }

    /**
     * Initializes this collection by deserializing a list of Extra Fields
     * of {@code size} bytes from the
     * byte array {@code data} at the zero based offset {@code off}.
     * Upon return, this collection shall not access {@code data}
     * subsequently and {@link #getExtraLength} must equal {@code size}.
     *
     * @param data The byte array to read the list of Extra Fields from.
     * @param off The zero based offset in the byte array where the first byte
     *        of the list of Extra Fields is read from.
     * @param size The length of the list of Extra Fields in bytes.
     * @throws IndexOutOfBoundsException If the byte array
     *         {@code data} does not hold at least {@code size}
     *         bytes at the zero based offset {@code off}.
     * @throws RuntimeException If {@code size} is illegal or the
     *         deserialized list of Extra Fields contains illegal data.
     * @see #getExtraLength
     */
    void readFrom(final byte[] data, int off, final int size) {
        UShort.check(size, "Extra Field out of range", null);
        final Map map = new TreeMap();
        if (data != null && size > 0) {
            final int end = off + size;
            while (off < end) {
                final int headerID = LittleEndian.readUShort(data, off);
                off += 2;
                final int dataSize = LittleEndian.readUShort(data, off);
                off += 2;
                final ExtraField ef = ExtraField.create(headerID);
                ef.readFrom(data, off, dataSize);
                off += dataSize;
                // TODO: For JSE 5: map.put(Integer.valueOf(headerID), ef);
                map.put(new Integer(headerID), ef);
            }
            assert off == end;
        }
        extra = map;
    }

    /**
     * Serializes a list of Extra Fields of {@link #getExtraLength} bytes to the
     * byte array {@code data} at the zero based offset {@code off}.
     * Upon return, this collection shall not access {@code data}
     * subsequently.
     *
     * @param data The byte array to write the list of Extra Fields to.
     * @param off The zero based offset in the byte array where the first byte
     *        of the list of Extra Fields is written to.
     * @throws IndexOutOfBoundsException If the byte array
     *         {@code data} does not hold at least {@link #getExtraLength}
     *         bytes at the zero based offset {@code off}.
     * @see #getExtraLength
     */
    void writeTo(final byte[] data, int off) {
       for (final Iterator it = extra.values().iterator(); it.hasNext(); ) {
            final ExtraField ef = (ExtraField) it.next();
            LittleEndian.writeShort(ef.getHeaderID(), data, off);
            off += 2;
            LittleEndian.writeShort(ef.getDataSize(), data, off);
            off += 2;
            ef.writeTo(data, off);
            off += ef.getDataSize();
        }
    }
}