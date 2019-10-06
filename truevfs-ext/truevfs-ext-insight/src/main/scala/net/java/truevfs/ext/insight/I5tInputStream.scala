/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.insight

import java.io._
import javax.annotation._
import javax.annotation.concurrent._
import net.java.truevfs.comp.inst._
import net.java.truevfs.comp.jmx._

/**
  * A controller for an [[java.io.InputStream]].
  *
  * @author Christian Schlichtherle
  */
@NotThreadSafe
private final class I5tInputStream(mediator: I5tMediator, @WillCloseWhenClosed in: InputStream)
  extends InstrumentingInputStream(mediator, in)
    with JmxComponent {

  override def activate(): Unit = {
  }

  override def read(): Int = {
    val start = System.nanoTime
    val ret = in.read()
    if (0 <= ret) {
      mediator.logRead(System.nanoTime - start, 1)
    }
    ret
  }

  override def read(b: Array[Byte], off: Int, len: Int): Int = {
    val start = System.nanoTime
    val ret = in.read(b, off, len)
    if (0 <= ret) {
      mediator.logRead(System.nanoTime - start, ret)
    }
    ret
  }
}
