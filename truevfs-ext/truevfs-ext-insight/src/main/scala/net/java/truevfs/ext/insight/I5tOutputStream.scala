/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.insight

import java.io._
import javax.annotation._
import javax.annotation.concurrent._
import net.java.truevfs.comp.inst._
import net.java.truevfs.comp.jmx._

/**
 * A controller for an [[java.io.OutputStream]].
 *
 * @author Christian Schlichtherle
 */
@NotThreadSafe
private final class I5tOutputStream(
  mediator: I5tMediator, @WillCloseWhenClosed out: OutputStream
) extends InstrumentingOutputStream(mediator, out) with JmxColleague {

  override def start() { }

  override def write(b: Int) {
    val start = System.nanoTime
    out write b
    mediator logWrite (System.nanoTime - start, 1)
  }

  override def write(b: Array[Byte], off: Int, len: Int) {
    val start = System.nanoTime
    out write (b, off, len)
    mediator logWrite (System.nanoTime - start, len)
  }
}
