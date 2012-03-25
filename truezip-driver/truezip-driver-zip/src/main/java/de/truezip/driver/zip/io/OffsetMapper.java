/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.io;

/**
 * Maps a given offset to a file pointer position.
 *
 * @author Christian Schlichtherle
 */
class OffsetMapper {
    long map(long offset) {
        return offset;
    }

    long unmap(long offset) {
        return offset;
    }
}