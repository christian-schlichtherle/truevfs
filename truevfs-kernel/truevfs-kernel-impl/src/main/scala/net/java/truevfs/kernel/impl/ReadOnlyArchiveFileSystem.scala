/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl

import net.java.truecommons.shed._
import javax.annotation.concurrent._
import net.java.truevfs.kernel.spec._
import net.java.truecommons.cio._
import net.java.truecommons.cio.Entry._
import net.java.truecommons.cio.Entry.Access._

/** A read-only virtual file system for archive entries.
  *
  * All modifying methods throw a
  * [[net.java.truevfs.kernel.impl.FsReadOnlyFileSystemException]].
  *
  * @param  <E> The type of the archive entries.
  * @author Christian Schlichtherle
  */
@NotThreadSafe
private final class ReadOnlyArchiveFileSystem[E <: FsArchiveEntry](
  model: ArchiveModel[E],
  archive: Container[E],
  rootTemplate: Option[Entry])
extends ArchiveFileSystem(model, archive, rootTemplate) {
  import ReadOnlyArchiveFileSystem._

  override def checkAccess(options: AccessOptions, name: FsNodeName, types: BitField[Access]) {
    if (!types.isEmpty && READ_ONLY != types)
        throw new FsReadOnlyFileSystemException(mountPoint)
    super.checkAccess(options, name, types)
  }

  override def setReadOnly(options: AccessOptions, name: FsNodeName) { }

  override def setTime(options: AccessOptions, name: FsNodeName, times: Map[Access, Long]) =
    throw new FsReadOnlyFileSystemException(mountPoint)

  override def setTime(options: AccessOptions, name: FsNodeName, types: BitField[Access], value: Long) =
    throw new FsReadOnlyFileSystemException(mountPoint)

  override def make(options: AccessOptions, name: FsNodeName, tµpe: Type, template: Option[Entry]) =
    throw new FsReadOnlyFileSystemException(mountPoint)

  override def unlink(options: AccessOptions, name: FsNodeName) =
    throw new FsReadOnlyFileSystemException(mountPoint)
}

private object ReadOnlyArchiveFileSystem {
  private val READ_ONLY = BitField.of(READ)
}
