/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.logging

import java.io._
import net.java.truevfs.kernel.spec.cio._

private trait LogCloseable extends Closeable with LogResource {

  log("Opened {}")

  abstract override def close {
    log("Closing {}")
    super.close
  }

  def log(message: String) {
    try {
      log(message, origin target ())
    } catch {
      case ex: IOException =>
        logger trace ("Couldn't resolve resource target: ", ex)
    }
  }

  def origin: IoSocket[_ <: Entry]
}
