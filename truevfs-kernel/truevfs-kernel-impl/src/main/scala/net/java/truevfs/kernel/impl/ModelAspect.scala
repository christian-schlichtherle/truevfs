/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl

import javax.annotation.concurrent._
import net.java.truevfs.kernel.spec._

/** A generic mixin which provides some features of its associated `model`.
  *
  * @tparam M the type of the generic file system model.
  * @author Christian Schlichtherle
  */
@ThreadSafe
private trait ModelAspect[+M <: Model] {

  /** The model with the features to provide as an aspect. */
  def model: M

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

  /** Composes the node path from the mountpoint of this model and the given
    * node name.
    * 
    * @param name the node name.
    */
  final def path(name: FsNodeName) = model path name
}
