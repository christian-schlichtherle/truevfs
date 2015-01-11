/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl

import javax.annotation.concurrent.ThreadSafe

import net.java.truevfs.kernel.impl.ShutdownFuse._

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

  @volatile
  private[this] var _armed: Boolean = _

  private[this] val _thread = new Thread {
    override def run() {
      // HC SVNT DRACONES!
      // MUST void any calls to disarm() during shutdown hook execution!
      onDisarm {
        hook // could call disarm()!
      }
    }
  }

  if (armed) { arm() }

  /** Arms this shutdown fuse. */
  def arm() { onArm { registry add _thread } }

  /** Disarms this shutdown fuse. */
  def disarm() { onDisarm { registry remove _thread } }

  @inline
  private[this] def onArm(block: => Unit) {
    onCondition(!_armed) {
      _armed = true
      block
    }
  }

  @inline
  private[this] def onDisarm(block: => Unit) {
    onCondition(_armed) {
      _armed = false
      block
    }
  }

  @inline
  private[this] def onCondition(condition: => Boolean)(block: => Unit) {
    if (condition) { synchronized { if (condition) { block } } }
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
