/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.logging

import java.io._
import net.java.truecommons.cio._

private trait LogCloseable extends Closeable with LogResource {

  log("Opened {}")

  abstract override def close(): Unit = {
    log("Closing {}")
    super.close
  }

  def log(message: String): Unit = {
    try {
      log(message, context.target())
    } catch {
      case ex: IOException =>
        logger trace ("Couldn't resolve resource target: ", ex)
    }
  }

  def context: IoSocket[_ <: Entry]
}
