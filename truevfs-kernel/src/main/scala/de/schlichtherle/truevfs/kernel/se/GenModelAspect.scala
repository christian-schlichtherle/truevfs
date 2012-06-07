/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truevfs.kernel.se

import javax.annotation.concurrent._
import net.truevfs.kernel._

/** A generic mixin which provides some features of its associated `model`.
  *
  * @tparam M the type of the file system model.
  * @author Christian Schlichtherle
  */
@ThreadSafe
private trait GenModelAspect[M <: FsModel] {

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
  final def touched = model.isTouched

  /** Sets the `touched` property of the (federated virtual) file system.
    *
    * @param touched the `touched` property of the (federated virtual) file system.
    */
  final def touched_=(touched: Boolean) { model setTouched touched }
}
