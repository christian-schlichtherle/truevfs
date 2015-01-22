/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec;

import net.java.truecommons.cio.MutableEntry;

/**
 * Represents an entry in an archive file.
 * Archive drivers need to implement this interface in order to enable their
 * client (i.e. archive controllers) to read and write archive entries from
 * and to archive files of their respective supported type.
 * <p>
 * Implementations do <em>not</em> need to be thread-safe.
 * 
 * @author Christian Schlichtherle
 */
public interface FsArchiveEntry extends MutableEntry {

    /**
     * Returns the type of this archive entry.
     *
     * @return The type of this archive entry.
     */
    Type getType();
}
