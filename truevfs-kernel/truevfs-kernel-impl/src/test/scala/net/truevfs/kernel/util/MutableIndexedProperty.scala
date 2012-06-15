/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.util

trait MutableIndexedProperty[-A, B] extends (A => B) {
  def update(index: A, value: B)
}
