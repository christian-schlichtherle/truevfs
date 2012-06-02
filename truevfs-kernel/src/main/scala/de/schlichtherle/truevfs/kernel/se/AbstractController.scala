/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truevfs.kernel.se

import net.truevfs.kernel._

private abstract class AbstractController[M <: FsModel](final val model: M)
extends FsController[M] {

  final override def getModel: M = model

  /**
   * Two file system controllers are considered equal if and only if
   * they are identical.
   * 
   * @param  that the object to compare.
   * @return {@code this == that}. 
   */
  final override def equals(that: Any) = this == that

  /**
   * Returns a hash code which is consistent with {@link #equals}.
   * 
   * @return A hash code which is consistent with {@link #equals}.
   * @see Object#hashCode
   */
  final override def hashCode = super.hashCode

  /**
   * Returns a string representation of this object for debugging and logging
   * purposes.
   * 
   * @return A string representation of this object for debugging and logging
   *         purposes.
   */
  override def toString = "%s[model=%s]".format(getClass.getName, model);
} // AbstractController
