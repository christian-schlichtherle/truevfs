/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.zip;

/**
 * Adds a start value to the given offset.
 *
 * @author  Christian Schlichtherle
 */
final class IrregularOffsetMapper extends OffsetMapper {
    final long start;

    IrregularOffsetMapper(final long start) {
        this.start = start;
    }

    @Override
    long map(long offset) {
        return offset + start;
    }

    @Override
    long unmap(long offset) {
        return offset - start;
    }
}