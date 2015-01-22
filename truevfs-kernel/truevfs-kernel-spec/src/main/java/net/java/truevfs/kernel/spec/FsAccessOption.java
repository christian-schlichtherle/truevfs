/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec;

import java.io.IOException;
import javax.annotation.concurrent.Immutable;

/**
 * Defines common access options for I/O operations.
 * Not all options may be supported or available for all operations and
 * certain combinations may even be illegal.
 * It's up to the particular operation and file system driver implementation
 * to define which options are supported and available.
 * If an option is not supported, it should get silently ignored.
 * If an option is not available or illegal, an {@link IOException} must get
 * thrown.
 *
 * @author Christian Schlichtherle
 */
@Immutable
public enum FsAccessOption {

    /** Whether or not a file system node must be exclusively created. */
    EXCLUSIVE,

    /**
     * Whether or not the contents of an existing file system node shall get
     * kept for appending rather than replaced for overwriting.
     */
    APPEND,

    /**
     * Whether or not the entry data shall get cached for subsequent access.
     * As a desired side effect, caching allows a federated file system (i.e.
     * prospective archive file) to {@link FsController#sync} its contents to
     * its parent file system while some client is still busy on reading
     * or writing the cached entry data.
     */
    CACHE,

    /**
     * Whether or not any missing parent directory entries shall get created
     * automatically with an undefined last modification time.
     */
    CREATE_PARENTS,

    /**
     * Expresses a preference to allow an archive file to grow by appending any
     * new or updated archive entry contents or meta data to its end.
     * Setting this option may produce redundant data in the resulting archive
     * file.
     * However, it may yield much better performance if the number and contents
     * of the archive entries to create or update are rather small compared to
     * the total size of the resulting archive file.
     * <p>
     * This option is the equivalent to a multi-session disc (CD, DVD etc.)
     * for archive files.
     * <p>
     * Note that this option may get ignored by archive file system drivers.
     * Furthermore, if this happens, there may be no direct feedback available
     * to the caller.
     */
    GROW,

    /**
     * Expresses a preference to store an entry uncompressed within its archive.
     * <p>
     * Note that this option may get ignored by archive file system drivers.
     * Furthermore, if this happens, there may be no direct feedback available
     * to the caller.
     */
    STORE,

    /**
     * Expresses a preference to compress an entry within its archive.
     * <p>
     * Note that this option may get ignored by archive file system drivers.
     * Furthermore, if this happens, there may be no direct feedback available
     * to the caller.
     */
    COMPRESS,

    /**
     * Expresses a preference to encrypt archive entries when writing them to
     * an archive file.
     * <p>
     * Note that this option may get ignored by archive file system drivers.
     * Furthermore, if this happens, there may be no direct feedback available
     * to the caller.
     */
    ENCRYPT,
}
