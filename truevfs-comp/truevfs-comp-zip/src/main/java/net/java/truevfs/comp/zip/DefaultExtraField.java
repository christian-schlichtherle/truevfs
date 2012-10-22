/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.zip;

import java.nio.ByteOrder;
import javax.annotation.concurrent.NotThreadSafe;
import net.java.truecommons.io.MutableBuffer;

/**
 * Default implementation of an Extra Field.
 *
 * @author Christian Schlichtherle
 */
@NotThreadSafe
class DefaultExtraField implements ExtraField {

    /**
     * A mutable mb which holds the Header Id, Data Size and Data Block.
     * The buffer is initialized with its position set to zero, an undefined
     * mark and its limit set to its capacity.
     * These properties are not used by the methods in the class
     * {@link DefaultExtraField}, so a subclass may freely use them.
     */
    protected final MutableBuffer mb;

    DefaultExtraField(final MutableBuffer mb) {
        assert mb.order() == ByteOrder.LITTLE_ENDIAN;
        final MutableBuffer b = mb.asReadOnlyBuffer().slice();
        final int totalSize = 4 + b.getUShort(2);
        UShort.check(totalSize);
        this.mb = b.limit(totalSize).slice();
        mb.position(mb.position() + totalSize);
    }

    DefaultExtraField(final int headerId, final int dataSize) {
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
    public int getTotalSize() { return 4 + getDataSize(); }

    @Override
    public int getHeaderId() { return mb.getUShort(0); }

    @Override
    public int getDataSize() { return mb.getUShort(2); }

    @Override
    public MutableBuffer totalBlock() {
        return mb.asReadOnlyBuffer().clear();
    }

    @Override
    public MutableBuffer dataBlock() {
        return totalBlock().position(4).slice();
    }

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
        protected ExtraField newExtraFieldUnchecked(MutableBuffer buf) {
            return new DefaultExtraField(buf);
        }
    } // Factory
}
