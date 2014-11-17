/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl

import java.io.IOException

import net.java.truecommons.shed._
import javax.annotation.concurrent._
import net.java.truevfs.kernel.spec._
import net.java.truecommons.cio._
import net.java.truecommons.cio.Entry._
import net.java.truecommons.cio.Entry.Access._
import scala.Option

/** A read-only virtual file system for archive entries.
  *
  * All modifying methods throw a
  * [[net.java.truevfs.kernel.spec.FsReadOnlyFileSystemException]].
  *
  * @param  <E> The type of the archive entries.
  * @author Christian Schlichtherle
  */
@NotThreadSafe
private final class ReadOnlyArchiveFileSystem[E <: FsArchiveEntry](
  model: ArchiveModel[E],
  archive: Container[E],
  rootTemplate: Option[Entry],
  cause: Throwable)
extends ArchiveFileSystem(model, archive, rootTemplate) {
  import ReadOnlyArchiveFileSystem._

  override def checkAccess(options: AccessOptions, name: FsNodeName, types: BitField[Access]) {
    if (!types.isEmpty && READ_ONLY != types)
        throw new FsReadOnlyFileSystemException(mountPoint, cause)
    super.checkAccess(options, name, types)
  }

  override def setReadOnly(options: AccessOptions, name: FsNodeName) { }

  override def setTime(options: AccessOptions, name: FsNodeName, times: Map[Access, Long]) =
    throw new FsReadOnlyFileSystemException(mountPoint, cause)

  override def setTime(options: AccessOptions, name: FsNodeName, types: BitField[Access], value: Long) =
    throw new FsReadOnlyFileSystemException(mountPoint, cause)

  override def make(options: AccessOptions, name: FsNodeName, tµpe: Type, template: Option[Entry]) =
    throw new FsReadOnlyFileSystemException(mountPoint, cause)

  override def unlink(options: AccessOptions, name: FsNodeName) =
    throw new FsReadOnlyFileSystemException(mountPoint, cause)
}

private object ReadOnlyArchiveFileSystem {
  private val READ_ONLY = BitField.of(READ)
}
