/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.logging

import net.java.truevfs.comp.inst._
import net.java.truecommons.cio._
import org.slf4j._

/**
 * @author Christian Schlichtherle
 */
private final class LogBuffer(director: LogMediator, buffer: IoBuffer)
extends InstrumentingBuffer[LogMediator](director, buffer) with LogResource {

  override def logger = LogBuffer.logger

  log("Allocated {}", entry)

  override def release {
    log("Releasing {}", entry)
    entry.release
  }
}

private object LogBuffer {
  private val logger = LoggerFactory.getLogger(classOf[LogBuffer])
}
