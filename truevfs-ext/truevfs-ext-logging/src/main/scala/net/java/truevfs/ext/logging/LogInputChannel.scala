/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
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
  override val context: InputSocket[_ <: Entry],
  channel: SeekableByteChannel
) extends ReadOnlyChannel(channel) with LogCloseable {

  override def logger: Logger = LogInputChannel.logger
}

private object LogInputChannel {

  val logger: Logger = LoggerFactory.getLogger(classOf[LogInputChannel])
}
