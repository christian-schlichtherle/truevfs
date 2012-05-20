/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truevfs.kernel.se

import net.truevfs.kernel._
import net.truevfs.kernel.cio._
import net.truevfs.kernel.cio.Entry._;
import net.truevfs.kernel.cio.Entry.Access._;
import net.truevfs.kernel.util._

/**
 * A read-only virtual file system for archive entries.
 * <p>
 * All modifying methods throw a {@link FsReadOnlyFileSystemException}.
 *
 * @param  <E> The type of the archive entries.
 * @author Christian Schlichtherle
 */
private final class ReadOnlyArchiveFileSystem[E <: FsArchiveEntry](
  driver: FsArchiveDriver[E],
  archive: Container[E],
  rootTemplate: Option[Entry])
extends ArchiveFileSystem(driver, archive, rootTemplate) {
  import ReadOnlyArchiveFileSystem._

  override def checkAccess(options: AccessOptions, name: FsEntryName, types: BitField[Access]) {
    if (!types.isEmpty && READ_ONLY != types)
        throw new FsReadOnlyFileSystemException();
    super.checkAccess(options, name, types)
  }

  override def setReadOnly(name: FsEntryName) { }

  override def setTime(options: AccessOptions, name: FsEntryName, times: Map[Access, Long]) =
    throw new FsReadOnlyFileSystemException

  override def setTime(options: AccessOptions, name: FsEntryName, types: BitField[Access], value: Long) =
    throw new FsReadOnlyFileSystemException

  override def mknod(options: AccessOptions, name: FsEntryName, tµpe: Type, template: Option[Entry]) =
    throw new FsReadOnlyFileSystemException

  override def unlink(options: AccessOptions, name: FsEntryName) =
    throw new FsReadOnlyFileSystemException
}

private object ReadOnlyArchiveFileSystem {
  private val READ_ONLY = BitField.of(READ)
}
