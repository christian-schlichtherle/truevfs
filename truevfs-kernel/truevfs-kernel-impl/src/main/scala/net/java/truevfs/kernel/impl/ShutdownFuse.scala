/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl

import java.util.concurrent.atomic.AtomicBoolean
import javax.annotation.concurrent.ThreadSafe

import scala.sys.runtime

/** Arms and disarms a configured shutdown hook.
  * A shutdown fuse allows to repeatedly register and remove its configured
  * shutdown hook for execution when the JVM shuts down.
  * The configured shutdown hook will only get executed when the JVM shuts down
  * and if the shutdown fuse is currently `arm`ed.
  *
  * @author Christian Schlichtherle
  */
@ThreadSafe
private final class ShutdownFuse private(initiallyArmed: Boolean, shutdownHook: => Unit) {

  private[this] val armed = new AtomicBoolean

  val thread = new Thread {
    override def run() {
      // HC SVNT DRACONES!
      // MUST void any calls to off() during shutdown hook execution!
      if (getAndSetArmed(false)) {
        shutdownHook // could call off()!
      }
    }
  }

  if (initiallyArmed) {
    arm()
  }

  @inline
  private[this] def getAndSetArmed = armed.getAndSet _

  /** Arms this shutdown fuse. */
  def arm() {
    if (!getAndSetArmed(true)) {
      runtime addShutdownHook thread
    }
  }

  /** Disarms this shutdown fuse. */
  def disarm() {
    if (getAndSetArmed(false)) {
      runtime removeShutdownHook thread
    }
  }
}

private object ShutdownFuse {

  @inline
  def apply(shutdownHook: => Unit): ShutdownFuse = apply(armed = true)(shutdownHook)

  @inline
  def apply(armed: Boolean)(shutdownHook: => Unit) = new ShutdownFuse(armed, shutdownHook)
}
