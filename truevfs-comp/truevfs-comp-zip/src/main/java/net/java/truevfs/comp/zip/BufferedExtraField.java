/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.zip;

import javax.annotation.concurrent.NotThreadSafe;
import net.java.truecommons.io.ImmutableBuffer;
import net.java.truecommons.io.MutableBuffer;

/**
 * An Extra Field which uses a byte buffer to hold its data.
 *
 * @author Christian Schlichtherle
 */
@NotThreadSafe
class BufferedExtraField implements ExtraField {

    /**
     * A mutable buffer which holds the data of this Extra Field.
     * The buffer is initialized with its position set to zero and its limit
     * set so that the interval fits the Header Id, Data Size and Data Block.
     * The buffer's mark and capacity are undefined.
     * The buffer's byte order is little-endian.
     */
    protected final MutableBuffer mb;

    /**
     * Constructs a new buffered extra field which shares the Header Id,
     * Data Size and Data Block with given immutable buffer.
     *
     * @param  ib the immutable buffer with the shared Header Id, Data Size and
     *         Data Block.
     * @throws RuntimeException if the buffer's content does not conform to the
     *         ZIP File Format Specification.
     */
    BufferedExtraField(final ImmutableBuffer ib) {
        final MutableBuffer mb = ib
                .asMutableBuffer()
                .slice()
                .littleEndian();
        final int totalSize = 4 + mb.getUShort(2);
        UShort.check(totalSize);
        this.mb = mb.limit(totalSize);
        mb.position(mb.position() + totalSize);
    }

    BufferedExtraField(final int headerId, final int dataSize) {
        UShort.check(headerId);
        UShort.check(dataSize);
        final int totalSize = 4 + dataSize;
        UShort.check(totalSize);
        this.mb = MutableBuffer
                .allocateDirect(totalSize)
                .littleEndian()
                .putShort((short) headerId)
                .putShort((short) dataSize);
    }

    @Override
    public int getHeaderId() { return mb.getUShort(0); }

    @Override
    public int getDataSize() { return getTotalSize() - 4; }

    @Override
    public MutableBuffer dataBlock() { return mb.clone().position(4); }

    @Override
    public int getTotalSize() { return mb.limit(); }

    @Override
    public MutableBuffer totalBlock() { return mb.clone().position(0); }

    protected boolean requireHeaderId(int headerId) {
        return validate(getHeaderId() == headerId, "%d (invalid Header Id)", headerId);
    }

    protected boolean requireDataSize(int dataSize) {
        return validate(getDataSize() == dataSize, "%d (invalid Data Size)", dataSize);
    }

    protected static boolean validate(boolean assertion) {
        return validate(assertion, "Invalid Extra Field data!");
    }

    protected static boolean validate(
            final boolean assertion,
            final String format,
            final Object... args) {
        if (assertion) return true;
        throw new IllegalArgumentException(String.format(format, args));
    }

    @SuppressWarnings("PackageVisibleInnerClass")
    static final class Factory extends AbstractExtraFieldFactory {
        @Override
        protected ExtraField newExtraFieldUnchecked(ImmutableBuffer ib) {
            return new BufferedExtraField(ib);
        }
    } // Factory
}
