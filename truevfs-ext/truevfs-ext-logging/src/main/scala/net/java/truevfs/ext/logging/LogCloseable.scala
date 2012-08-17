/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.logging

import java.io._
import net.java.truevfs.kernel.spec.cio._

private trait LogCloseable extends Closeable with LogResource {

  def origin: IoSocket[_ <: Entry]

  def log(message: String) {
    val entry = {
      try { origin target () }
      catch { case _: IOException => null }
    }
    log(message, entry)
  }

  log("Opened {}")

  abstract override def close {
    log("Closing {}")
    super.close
  }
}
