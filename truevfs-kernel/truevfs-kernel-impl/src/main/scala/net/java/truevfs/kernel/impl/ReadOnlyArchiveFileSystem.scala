/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl

import javax.annotation.concurrent._
import net.java.truecommons.cio.Entry.Access._
import net.java.truecommons.cio.Entry._
import net.java.truecommons.cio._
import net.java.truecommons.shed._
import net.java.truevfs.kernel.impl.ReadOnlyArchiveFileSystem._
import net.java.truevfs.kernel.spec._

import scala.Option

/** A read-only virtual file system for archive entries.
  *
  * All modifying methods throw a
  * [[net.java.truevfs.kernel.spec.FsReadOnlyFileSystemException]].
  *
  * @tparam E The type of the archive entries.
  * @author Christian Schlichtherle
  */
@NotThreadSafe
private final class ReadOnlyArchiveFileSystem[E <: FsArchiveEntry]
(model: ArchiveModel[E], archive: Container[E], rootTemplate: Entry, cause: Throwable)
  extends ArchiveFileSystem(model, archive, rootTemplate) {

  override def checkAccess(options: AccessOptions, name: FsNodeName, types: BitField[Access]): Unit = {
    if (!types.isEmpty && READ_ONLY != types) {
      throw new FsReadOnlyFileSystemException(mountPoint, cause)
    }
    super.checkAccess(options, name, types)
  }

  override def setReadOnly(options: AccessOptions, name: FsNodeName): Unit = {
  }

  override def setTime(options: AccessOptions, name: FsNodeName, times: Map[Access, Long]): Boolean = {
    throw new FsReadOnlyFileSystemException(mountPoint, cause)
  }

  override def setTime(options: AccessOptions, name: FsNodeName, types: BitField[Access], value: Long): Boolean = {
    throw new FsReadOnlyFileSystemException(mountPoint, cause)
  }

  override def make(options: AccessOptions, name: FsNodeName, tµpe: Type, template: Option[Entry]): Make = {
    throw new FsReadOnlyFileSystemException(mountPoint, cause)
  }

  override def unlink(options: AccessOptions, name: FsNodeName): Unit = {
    throw new FsReadOnlyFileSystemException(mountPoint, cause)
  }
}

private object ReadOnlyArchiveFileSystem {

  private val READ_ONLY = BitField.of(READ)
}
