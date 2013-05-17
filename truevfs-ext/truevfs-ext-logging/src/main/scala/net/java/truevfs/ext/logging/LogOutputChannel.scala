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
private final class LogOutputChannel(
  override val origin: OutputSocket[_ <: Entry],
  channel: SeekableByteChannel)
extends DecoratingSeekableChannel(channel) with LogCloseable with Immutable {
  override def logger = LogOutputChannel.logger
}

private object LogOutputChannel {
  private val logger = LoggerFactory.getLogger(classOf[LogOutputChannel])
}
