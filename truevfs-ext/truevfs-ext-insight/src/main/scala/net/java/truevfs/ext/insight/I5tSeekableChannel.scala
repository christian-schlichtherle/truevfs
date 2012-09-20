/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.insight

import java.nio._
import java.nio.channels._
import javax.annotation._
import javax.annotation.concurrent._
import net.java.truevfs.comp.inst._
import net.java.truevfs.comp.jmx._

/**
 * A controller for a [[java.nio.channels.SeekableByteChannel]].
 *
 * @author Christian Schlichtherle
 */
@NotThreadSafe
private final class I5tSeekableChannel(
  mediator: I5tMediator, @WillCloseWhenClosed channel: SeekableByteChannel
) extends InstrumentingSeekableChannel(mediator, channel) with JmxColleague {

  override def start { }

  override def read(buf: ByteBuffer) = {
    val start = System.nanoTime
    val ret = channel read buf
    if (0 <= ret) mediator logRead (System.nanoTime - start, ret)
    ret
  }

  override def write(buf: ByteBuffer) = {
    val start = System.nanoTime
    val ret = channel write buf
    mediator logWrite (System.nanoTime - start, ret)
    ret
  }
}
