/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl

import java.util.concurrent.atomic.AtomicBoolean
import javax.annotation.concurrent.ThreadSafe
import ShutdownFuse._

/** Arms and disarms a configured shutdown hook.
  * A shutdown fuse allows to repeatedly register and remove its configured
  * shutdown hook for execution when the JVM shuts down.
  * The configured shutdown hook will only get executed when the JVM shuts down
  * and if the shutdown fuse is currently `arm`ed.
  *
  * @author Christian Schlichtherle
  */
@ThreadSafe
private final class ShutdownFuse private (armed: Boolean, registry: ThreadRegistry, hook: => Unit) {

  private[this] val _armed = new AtomicBoolean

  private[this] val _thread = new Thread {
    override def run() {
      // HC SVNT DRACONES!
      // MUST void any calls to disarm() during shutdown hook execution!
      if (getAndSetArmed(false)) {
        hook // could call disarm()!
      }
    }
  }

  if (armed) {
    arm()
  }

  @inline
  private[this] def getAndSetArmed = _armed.getAndSet _

  /** Arms this shutdown fuse. */
  def arm() {
    if (!getAndSetArmed(true)) {
      registry add _thread
    }
  }

  /** Disarms this shutdown fuse. */
  def disarm() {
    if (getAndSetArmed(false)) {
      registry remove _thread
    }
  }

  /** For testing only! */
  private[impl] def blowUp() { _thread run () }
}

private object ShutdownFuse {

  @inline
  def apply(hook: => Unit): ShutdownFuse = apply()(hook)

  @inline
  def apply(armed: Boolean = true, registry: ThreadRegistry = DefaultThreadRegistry)(hook: => Unit) = new ShutdownFuse(armed, registry, hook)

  sealed trait ThreadRegistry {
    def add(thread: Thread)
    def remove(thread: Thread)
  }

  object DefaultThreadRegistry extends ThreadRegistry {

    def add(thread: Thread) {
      try {
        Runtime.getRuntime addShutdownHook thread
      } catch {
        case theHookCouldNotArmTheFuse: IllegalStateException => // ignore
      }
    }

    def remove(thread: Thread) {
      try {
        Runtime.getRuntime removeShutdownHook thread
      } catch {
        case theHookCouldNotDisarmTheFuse: IllegalStateException => // ignore
      }
    }
  }
}
