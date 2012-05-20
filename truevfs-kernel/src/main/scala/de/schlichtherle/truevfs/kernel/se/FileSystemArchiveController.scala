/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truevfs.kernel.se

import net.truevfs.kernel._
import net.truevfs.kernel.cio._
import net.truevfs.kernel.cio.Entry._;
import net.truevfs.kernel.io._
import net.truevfs.kernel.util._
import java.io._
import java.nio.channels._

/**
 * This abstract archive controller controls the mount state transition.
 * It is up to the sub class to implement the actual mounting/unmounting
 * strategy.
 * <p>
 * Note that all {@link FsController} API methods may throw a
 * {@link ControlFlowException}, for example when
 * {@linkplain FalsePositiveException detecting a false positive archive file}, or
 * {@linkplain NeedsWriteLockException requiring a write lock} or
 * {@linkplain NeedsSyncException requiring a sync}.
 *
 * @param  <E> the type of the archive entries.
 * @author Christian Schlichtherle
 */
private abstract class FileSystemArchiveController[E <: FsArchiveEntry](
  m: LockModel)
extends BasicArchiveController[E](m)
with LockModelController
with MountState[E] {

  /** The mount state of the archive file system. */
  private[this] var mountState: MountState[E] = new ResetFileSystem

  final def autoMount(options: AccessOptions, autoCreate: Boolean) =
    mountState.autoMount(options, autoCreate)

  final def fileSystem = mountState.fileSystem

  final def fileSystem_=(fileSystem: Option[ArchiveFileSystem[E]]) {
    mountState.fileSystem = fileSystem
  }

  /**
   * Mounts the (virtual) archive file system from the target file.
   * <p>
   * Upon normal termination, this method is expected to have called
   * {@link #setFileSystem} to assign the fully initialized file system
   * to this controller.
   * Other than this, the method must not have any side effects on the
   * state of this class or its super class.
   * It may, however, have side effects on the state of the sub class.
   * <p>
   * The implementation may safely assume that the write lock for the file
   * system is acquired.
   *
   * @param  options the options for accessing the file system entry.
   * @param  autoCreate If this is {@code true} and the archive file does not
   *         exist, then a new archive file system with only a virtual root
   *         directory is created with its last modification time set to the
   *         system's current time.
   * @throws IOException on any I/O error.
   */
  def mount(options: AccessOptions, autoCreate: Boolean)

  private final class ResetFileSystem extends MountState[E] {
    override def autoMount(options: AccessOptions, autoCreate: Boolean) = {
      checkWriteLockedByCurrentThread
      mount(options, autoCreate)
      mountState.fileSystem.get
    }

    override def fileSystem = None

    override def fileSystem_=(fileSystem: Option[ArchiveFileSystem[E]]) {
      // Passing in None may happen by sync(*).
      fileSystem.foreach { fs => mountState = new MountedFileSystem(fs) }
    }
  } // ResetFileSystem

  private final class MountedFileSystem(fs: ArchiveFileSystem[E])
  extends MountState[E] {
    override def autoMount(options: AccessOptions, autoCreate: Boolean) =
      fs

    override def fileSystem = Some(fs)

    override def fileSystem_=(fileSystem: Option[ArchiveFileSystem[E]]) {
      fileSystem match {
        case Some(fs) => throw new IllegalStateException("File system already mounted!")
        case _ => mountState = new ResetFileSystem
      }
    }
  } // MountedFileSystem
}

/** Represents the mount state of the archive file system. */
private sealed trait MountState[E <: FsArchiveEntry] {
  def autoMount(options: AccessOptions, autoCreate: Boolean): ArchiveFileSystem[E]
  var fileSystem: Option[ArchiveFileSystem[E]]
} // MountState
