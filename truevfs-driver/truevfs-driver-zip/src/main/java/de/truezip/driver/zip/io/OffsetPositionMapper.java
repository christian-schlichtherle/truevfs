/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.io;

/**
 * Adds a offset value to the given position.
 *
 * @author Christian Schlichtherle
 */
final class OffsetPositionMapper extends PositionMapper {
    final long offset;

    OffsetPositionMapper(final long offset) {
        this.offset = offset;
    }

    @Override
    long map(long position) {
        return position + offset;
    }

    @Override
    long unmap(long position) {
        return position - offset;
    }
}
