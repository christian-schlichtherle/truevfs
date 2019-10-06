/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl

import java.io._
import javax.annotation.concurrent._

import net.java.truevfs.kernel.spec._

/** This abstract archive controller controls the mount state transition.
  * It is up to the sub-class to implement the actual mounting/unmounting
  * strategy.
  *
  * This controller is an emitter of
  * [[net.java.truecommons.shed.ControlFlowException]]s, for example
  * when
  * [[net.java.truevfs.kernel.impl.NeedsWriteLockException requiring a write lock]].
  *
  * @tparam E the type of the archive entries.
  * @author Christian Schlichtherle
  */
@NotThreadSafe
private abstract class FileSystemArchiveController[E <: FsArchiveEntry]
extends BasicArchiveController[E] with MountState[E] {
  controller: ArchiveModelAspect[E] =>

  /** The mount state of the archive file system. */
  private[this] var mountState: MountState[E] = new ResetFileSystem

  final def autoMount(options: AccessOptions, autoCreate: Boolean): ArchiveFileSystem[E] =
    mountState autoMount (options, autoCreate)

  final def fileSystem: Option[ArchiveFileSystem[E]] = mountState.fileSystem

  final def fileSystem_=(fileSystem: Option[ArchiveFileSystem[E]]): Unit = {
    mountState.fileSystem = fileSystem
  }

  /**
   * Mounts the (virtual) archive file system from the target file.
   * <p>
   * Upon normal termination, this method is expected to have called
   * `setFileSystem` to assign the fully initialized file system
   * to this controller.
   * Other than this, the method must not have any side effects on the
   * state of this class or its super class.
   * It may, however, have side effects on the state of the sub class.
   * <p>
   * The implementation may safely assume that the write lock for the file
   * system is acquired.
   *
   * @param  options the options for accessing the file system entry.
   * @param  autoCreate If this is `true` and the archive file does not
   *         exist, then a new archive file system with only a virtual root
   *         directory is created with its last modification time set to the
   *         system's current time.
   * @throws IOException on any I/O error.
   */
  def mount(options: AccessOptions, autoCreate: Boolean): Unit

  private final class ResetFileSystem extends MountState[E] {

    def autoMount(options: AccessOptions, autoCreate: Boolean): ArchiveFileSystem[E] = {
      checkWriteLockedByCurrentThread()
      mount(options, autoCreate)
      mountState.fileSystem.get
    }

    def fileSystem: Option[ArchiveFileSystem[E]] = None

    def fileSystem_=(fileSystem: Option[ArchiveFileSystem[E]]): Unit = {
      // Passing in None may happen by sync(*).
      fileSystem.foreach { fs => mountState = new MountedFileSystem(fs) }
    }
  }

  private final class MountedFileSystem(fs: ArchiveFileSystem[E]) extends MountState[E] {

    def autoMount(options: AccessOptions, autoCreate: Boolean): ArchiveFileSystem[E] = fs

    def fileSystem: Option[ArchiveFileSystem[E]] = Some(fs)

    def fileSystem_=(fileSystem: Option[ArchiveFileSystem[E]]): Unit = {
      fileSystem match {
        case Some(_) => throw new IllegalStateException("File system already mounted!")
        case _ => mountState = new ResetFileSystem
      }
    }
  }
}

/** Represents the mount state of the archive file system. */
private sealed trait MountState[E <: FsArchiveEntry] {

  def autoMount(options: AccessOptions, autoCreate: Boolean): ArchiveFileSystem[E]

  def fileSystem: Option[ArchiveFileSystem[E]]

  def fileSystem_=(fileSystem: Option[ArchiveFileSystem[E]]): Unit
}
