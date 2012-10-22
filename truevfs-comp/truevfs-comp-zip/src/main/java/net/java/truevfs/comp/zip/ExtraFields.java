/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.zip;

import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipException;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import net.java.truecommons.io.ImmutableBuffer;
import net.java.truecommons.io.MutableBuffer;

/**
 * Represents a collection of {@link ExtraField extra fields} as they may
 * be present at several locations in ZIP files.
 *
 * @author  Christian Schlichtherle
 */
@NotThreadSafe
final class ExtraFields implements Cloneable {

    private static final Map<Integer, ExtraFieldFactory>
            registry = new HashMap<>();

    /** The Header Id for a ZIP64 Extended Information extra field. */
    static final int ZIP64_HEADER_ID = 0x0001;

    static {
        register(WinZipAesExtraField.HEADER_ID, new WinZipAesExtraField.Factory());
    }

    private static void register(
            final int headerId,
            final ExtraFieldFactory factory) {
        UShort.check(headerId);
        registry.put(headerId, factory);
    }

    /**
     * The map of extra fields.
     * Maps from Header Id [{@link Integer}] to extra field [{@link ExtraField}].
     * Must not be {@code null}, but may be empty if no extra fields are used.
     * The map is sorted by Header IDs in ascending order.
     */
    private Map<Integer, ExtraField> fields = new TreeMap<>();

    /** Returns a shallow clone of this collection. */
    @Override
    @SuppressWarnings("AccessingNonPublicFieldOfAnotherObject")
    public ExtraFields clone() {
        try {
            final ExtraFields clone = (ExtraFields) super.clone();
            clone.fields = new TreeMap<>(fields);
            return clone;
        } catch (CloneNotSupportedException cannotHappen) {
            throw new AssertionError(cannotHappen);
        }
    }

    /** Returns the number of extra fields in this collection. */
    int size() { return fields.size(); }

    /**
     * Returns the extra field with the given Header HEADER_ID or {@code null}
     * if no such extra field exists.
     *
     * @param headerId The requested Header HEADER_ID.
     * @return The extra field with the given Header HEADER_ID or {@code null}
     *         if no such extra field exists.
     * @throws IllegalArgumentException If {@code headerID} is not in
     *         the range of {@code 0} to {@link UShort#MAX_VALUE}
     *         ({@value net.truevfs.driver.zip.io.UShort#MAX_VALUE}).
     */
    @CheckForNull ExtraField get(final int headerId) {
        assert UShort.check(headerId);
        final ExtraField ef = fields.get(headerId);
        assert null == ef || ef.getHeaderId() == headerId;
        return ef;
    }

    /**
     * Stores the given extra field in this collection.
     *
     * @param ef The extra field to store in this collection.
     * @return The extra field previously associated with the Header HEADER_ID of
     *         of the given extra field or {@code null} if no such
     *         extra field existed.
     * @throws NullPointerException If {@code ef} is {@code null}.
     * @throws IllegalArgumentException If the Header HEADER_ID of the given Extra
     *         Field is not in the range of {@code 0} to
     *         {@link UShort#MAX_VALUE}
     *         ({@value net.truevfs.driver.zip.io.UShort#MAX_VALUE}).
     */
    @Nullable ExtraField add(final ExtraField ef) {
        final int headerId = ef.getHeaderId();
        assert UShort.check(headerId);
        return fields.put(headerId, ef);
    }

    /**
     * Removes the extra field with the given Header HEADER_ID.
     *
     * @param headerId The requested Header HEADER_ID.
     * @return The extra field with the given Header HEADER_ID or {@code null}
     *         if no such extra field exists.
     * @throws IllegalArgumentException If {@code headerID} is not in
     *         the range of {@code 0} to {@link UShort#MAX_VALUE}
     *         ({@value net.truevfs.driver.zip.io.UShort#MAX_VALUE}).
     */
    @Nullable ExtraField remove(final int headerId) {
        assert UShort.check(headerId);
        final ExtraField ef = fields.remove(headerId);
        assert null == ef || ef.getHeaderId() == headerId;
        return ef;
    }

    /**
     * Returns the total number of bytes required to hold the extra fields.
     *
     * @return The length of the extra fields in bytes.
     *         May be {@code 0}.
     */
    int getTotalSize() {
        final Map<Integer, ExtraField> extra = this.fields;
        if (extra.isEmpty()) return 0;
        int l = 0;
        for (ExtraField ef : extra.values()) l += ef.getTotalSize();
        return l;
    }

    /**
     * Deserializes this collection of extra fields from the given immutable
     * buffer {@code ib}.
     */
    void parse(final ImmutableBuffer ib) throws ZipException {
        assert ib.order() == ByteOrder.LITTLE_ENDIAN;
        UShort.check(ib.remaining());
        final MutableBuffer mb = ib.asReadOnlyBuffer().asMutableBuffer();
        final Map<Integer, ExtraField> map = new TreeMap<>();
        while (0 < mb.remaining()) {
            final ExtraField ef = extraField(mb);
            map.put(ef.getHeaderId(), ef);
        }
        fields = map;
    }

    private static ExtraField extraField(final MutableBuffer buf)
    throws ZipException {
        final int headerId = buf.getUShort(buf.position()); // peek
        ExtraFieldFactory eff = registry.get(headerId);
        if (null == eff) eff = new BufferedExtraField.Factory();
        final ExtraField ef = eff.newExtraField(buf); // advances position
        assert headerId == ef.getHeaderId();
        return ef;
    }

    void compose(final MutableBuffer mb) {
        assert mb.order() == ByteOrder.LITTLE_ENDIAN;
        for (final ExtraField ef : fields.values()) mb.put(ef.totalBlock());
    }
}
