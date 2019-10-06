/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl

import java.util.concurrent.locks.ReentrantReadWriteLock

import net.java.truevfs.kernel.spec._

/** A generic mixin which provides some features of its associated `model`.
  *
  * @author Christian Schlichtherle
  */
private trait ArchiveModelAspect[E <: FsArchiveEntry]
extends ReentrantReadWriteLockAspect {

  final override def lock: ReentrantReadWriteLock = model.lock

  /** Returns the archive model with the features to provide as an aspect.
    *
    * @return The archive model with the features to provide as an aspect.
    */
  def model: ArchiveModel[E]

  /** Returns the mount point of the (federated virtual) file system.
    *
    * @return The mount point of the (federated virtual) file system.
    */
  final def mountPoint: FsMountPoint = model.getMountPoint

  /** Returns the `touched` property of the (federated virtual) file system.
    *
    * @return The `touched` property of the (federated virtual) file system.
    */
  final def mounted: Boolean = model.isMounted

  /** Sets the `touched` property of the (federated virtual) file system.
    *
    * @param mounted the `mounted` property of the (federated virtual) file system.
    */
  final def mounted_=(mounted: Boolean): Unit = { model setMounted mounted }

  final def driver: FsArchiveDriver[E] = model.driver

  /** Composes the node path from the mountpoint of this model and the given
    * node name.
    * 
    * @param name the node name.
    */
  final def path(name: FsNodeName): FsNodePath = model path name

  final def touch(options: AccessOptions): Unit = model touch options
}
