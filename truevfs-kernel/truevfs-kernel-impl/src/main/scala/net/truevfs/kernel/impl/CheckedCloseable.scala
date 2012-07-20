/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.impl

import edu.umd.cs.findbugs.annotations._
import java.io._
import javax.annotation.concurrent._
import net.truevfs.kernel.spec.io._

/**
  * @author Christian Schlichtherle
  */
@NotThreadSafe
private trait CheckedCloseable extends Closeable {
  private[this] var closed: Boolean = _

  /**
    * Closes this object.
    * Subsequent calls to this method will just forward the call to the
    * abstract super class.
    */
  @DischargesObligation
  final abstract override def close() { closed = true; super.close() }

  final def isOpen = !closed;
  protected def check() { if (!isOpen) throw new ClosedException }
  protected final def checked[V](operation: => V) = { check(); operation }
}
