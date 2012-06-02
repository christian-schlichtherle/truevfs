/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truevfs.kernel.se

import net.truevfs.kernel._

/**
 * A mixin which defines some methods on its associated {@link FsModel}.
 *
 * @see    FsModel
 * @author Christian Schlichtherle
 */
private trait ModelAspect[M <: FsModel] {

  def model: M

  /**
   * Returns the mount point of this (federated virtual) file system as
   * defined by the {@linkplain #getModel() model}.
   * 
   * @return The mount point of this (federated virtual) file system as
   *         defined by the {@linkplain #getModel() model}.
   */
  final def mountPoint = model getMountPoint

  /**
   * Returns the {@code touched} property of the
   * {@linkplain #getModel() file system model}.
   * 
   * @return the {@code touched} property of the
   *         {@linkplain #getModel() file system model}.
   */
  final def touched = model isTouched

  /**
   * Sets the {@code touched} property of the
   * {@linkplain #getModel() file system model}.
   * 
   * @param touched the {@code touched} property of the
   *         {@linkplain #getModel() file system model}.
   */
  final def touched_=(touched: Boolean) = model setTouched touched
}
