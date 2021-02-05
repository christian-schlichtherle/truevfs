/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.kernel.impl;

import global.namespace.truevfs.commons.shed.BitField;
import global.namespace.truevfs.kernel.api.FsAccessOption;
import global.namespace.truevfs.kernel.api.FsArchiveEntry;

import java.io.IOException;
import java.util.Optional;

/**
 * Defines the mount state of the archive file system.
 */
interface MountState<E extends FsArchiveEntry> {

    Optional<ArchiveFileSystem<E>> getFileSystem();

    void setFileSystem(Optional<ArchiveFileSystem<E>> fileSystem);

    ArchiveFileSystem<E> autoMount(BitField<FsAccessOption> options, boolean autoCreate) throws IOException;
}
