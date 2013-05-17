/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.logging

import java.nio.channels._
import net.java.truecommons.io._
import net.java.truecommons.cio._
import org.slf4j._

/**
 * @author Christian Schlichtherle
 */
private final class LogInputChannel(
  override val origin: InputSocket[_ <: Entry],
  channel: SeekableByteChannel
) extends ReadOnlyChannel(channel) with LogCloseable with Immutable {
  override def logger = LogInputChannel.logger
}

private object LogInputChannel {
  private val logger = LoggerFactory.getLogger(classOf[LogInputChannel])
}
