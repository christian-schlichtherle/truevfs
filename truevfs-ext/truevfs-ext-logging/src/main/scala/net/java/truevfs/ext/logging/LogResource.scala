/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.logging

import org.slf4j._

private trait LogResource {

  def logger: Logger

  def log(message: String, parameter: AnyRef): Unit = {
    logger debug (message, parameter)
    if (logger.isTraceEnabled) {
      logger.trace("Stack trace:", new Throwable)
    }
  }
}
