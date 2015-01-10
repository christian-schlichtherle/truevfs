/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl

import net.java.truecommons.shed._
import net.java.truevfs.kernel.spec._
import javax.annotation.concurrent._

/** A shutdown hook singleton which `sync`s a `register`ed file system manager
  * when it's `run`.
  * This is to protect an application from loss of data if the manager isn't
  * explicitly asked to `sync` before the JVM terminates.
  *
  * @author Christian Schlichtherle
  */
@ThreadSafe
private class SyncShutdownHook(manager: => FsManager) extends Thread {

  setPriority(Thread.MAX_PRIORITY)

  @volatile
  private var registered: Boolean = _

  /** Registers the configured file system `manager` for `sync`hronization when
    * the shutdown hook is `run`.
    *
    * @see   #deregister
    */
  def register() {
    if (!registered) {
      synchronized {
        if (!registered) {
          Runtime.getRuntime addShutdownHook this
          registered = true
        }
      }
    }
  }

  /** Deregisters the configured file system manager.
    *
    * @see #register
    */
  def deregister() {
    if (registered) {
      synchronized {
        if (registered) {
          // Prevent memory leak in dynamic class loader environments.
          Runtime.getRuntime removeShutdownHook this
          registered = false
        }
      }
    }
  }

  /** `sync`s the configured file system manager if and only if it's registered.
    * If any exception occurs while running this shutdown hook, its stacktrace
    * gets printed to standard error because logging doesn't always work in
    * shutdown hooks.
    *
    * @deprecated Do '''not''' call this method directly!
    * @see #register
    */
  override def run() {
    // HC SVNT DRACONES!
    if (registered) {
      // MUST void any calls to deregister() during a sync()!
      registered = false
      try {
        // This should call deregister()!
        manager sync (Filter.ACCEPT_ANY,
                      new FsControllerSyncVisitor(FsSyncOptions.UMOUNT))
      } catch {
        case ex: FsSyncException => ex printStackTrace ()
      }
    }
  }
}
