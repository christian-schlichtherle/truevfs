/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.kernel.api;

import global.namespace.truevfs.comp.cio.MutableEntry;

/**
 * Represents an entry in an archive file.
 * Archive drivers need to implement this interface in order to enable their
 * client (i.e. archive controllers) to read and write archive entries from
 * and to archive files of their respective supported type.
 * <p>
 * Implementations do not need to be thread-safe.
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
