/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl;

import net.java.truecommons.shed.UniqueObject;
import net.java.truevfs.kernel.spec.FsArchiveEntry;

import static java.util.Locale.ENGLISH;

abstract class AbstractArchiveController<E extends FsArchiveEntry> extends UniqueObject implements ArchiveController<E> {

    /**
     * Returns a string representation of this object for logging and debugging purposes.
     */
    @Override
    public final String toString() {
        return String.format(ENGLISH, "%s@%x[model=%s]", getClass().getName(), hashCode(), getModel());
    }
}
