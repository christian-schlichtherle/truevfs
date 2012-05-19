/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.zip.io;

/**
 * Maps a given position.
 *
 * @author Christian Schlichtherle
 */
class PositionMapper {
    long map(long position) {
        return position;
    }

    long unmap(long position) {
        return position;
    }
}
