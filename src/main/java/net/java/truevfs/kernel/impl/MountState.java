/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl;

import net.java.truecommons.shed.BitField;
import net.java.truevfs.kernel.spec.FsAccessOption;
import net.java.truevfs.kernel.spec.FsArchiveEntry;

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
