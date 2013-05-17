/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl

import net.java.truecommons.io._
import edu.umd.cs.findbugs.annotations._
import java.io._
import javax.annotation.concurrent._

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
  protected def check() { if (!isOpen) throw new ClosedStreamException }
  protected final def checked[V](operation: => V) = { check(); operation }
}
