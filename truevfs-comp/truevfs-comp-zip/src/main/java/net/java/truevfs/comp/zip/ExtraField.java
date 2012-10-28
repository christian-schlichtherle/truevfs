/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.zip;

import net.java.truecommons.io.MutableBuffer;

/**
 * An Extra Field in a Local or Central Header.
 *
 * @author Christian Schlichtherle
 */
interface ExtraField {

    /**
     * Returns the Header Id of this Extra Field.
     * The Header Id is an unsigned short integer (two bytes)
     * which indicates the type of this Extra Field.
     */
    int getHeaderId();

    /**
     * Returns the Data Size of this Extra Field.
     * The Data Size is an unsigned short integer (two bytes)
     * which indicates the length of the Data Block in bytes.
     */
    int getDataSize();

    /**
     * Returns a new mutable buffer for accessing the Data Block.
     * The returned buffer's position and limit are set so that the interval
     * fits the Data Block.
     * The buffer's mark and capacity are undefined.
     *
     * @return A new mutable buffer for accessing the Data Block.
     */
    MutableBuffer dataBlock();

    /**
     * Returns the Total Size of this Extra Field.
     */
    int getTotalSize();

    /**
     * Returns a new mutable buffer for accessing the Header Id, Data Size and
     * Data Block.
     * The returned buffer's position and limit are set so that the interval
     * fits the Header Id, Data Size and Data Block.
     * The buffer's mark and capacity are undefined.
     *
     * @return A new mutable buffer for accessing the Header Id, Data Size and
     *         Data Block.
     */
    MutableBuffer totalBlock();
}
