/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl

import net.java.truevfs.kernel.spec._

/** A generic mixin which provides some features of its associated `model`.
  *
  * @author Christian Schlichtherle
  */
private trait ArchiveModelAspect[E <: FsArchiveEntry]
extends ReentrantReadWriteLockAspect {

  final override def lock = model.lock

  /** Returns the archive model with the features to provide as an aspect.
    *
    * @return The archive model with the features to provide as an aspect.
    */
  def model: ArchiveModel[E]

  /** Returns the mount point of the (federated virtual) file system.
    *
    * @return The mount point of the (federated virtual) file system.
    */
  final def mountPoint = model.getMountPoint

  /** Returns the `touched` property of the (federated virtual) file system.
    *
    * @return The `touched` property of the (federated virtual) file system.
    */
  final def mounted = model.isMounted

  /** Sets the `touched` property of the (federated virtual) file system.
    *
    * @param touched the `touched` property of the (federated virtual) file system.
    */
  final def mounted_=(mounted: Boolean) { model setMounted mounted }

  final def driver = model.driver

  /** Composes the node path from the mountpoint of this model and the given
    * node name.
    * 
    * @param name the node name.
    */
  final def path(name: FsNodeName) = model path name

  final def touch(options: AccessOptions) = model touch options
}
