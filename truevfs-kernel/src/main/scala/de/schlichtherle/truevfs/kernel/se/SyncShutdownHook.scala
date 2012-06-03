/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truevfs.kernel.se

import net.truevfs.kernel._

/**
 * A shutdown hook singleton which {@linkplain FsManager#sync syncs} a
 * {@linkplain SyncShutdownHook#register registered} file system manager when
 * it's run.
 * This is to protect an application from loss of data if the manager isn't
 * explicitly asked to {@code sync()} before the JVM terminates.
 * 
 * @see    FsManager#sync
 * @author Christian Schlichtherle
 */
private object SyncShutdownHook extends Thread {
  setPriority(Thread.MAX_PRIORITY)

  @volatile
  private var _manager: Option[FsManager] = None

  /**
   * Registers the given file system {@code manager} for
   * {@linkplain FsManager#sync synchronization} when the shutdown hook is
   * {@linkplain #run run}.
   * 
   * @param manager the file system manager to
   *        {@linkplain FsManager#sync synchronize} when the shutdown hook
   *        is {@linkplain #run run}.
   * @see   #cancel
   */
  def register(manager: FsManager) {
    if (_manager.orNull != manager) {
      synchronized {
        if (_manager.orNull != manager) {
          Runtime.getRuntime addShutdownHook this
          _manager = Option(manager)
        }
      }
    }
  }

  /**
   * De-registers any previously registered file system manager.
   * 
   * @see #register
   */
  def cancel() {
    if (_manager isDefined) {
      synchronized {
        if (_manager isDefined) {
          // Prevent memory leak in dynamic class loader environments.
          Runtime.getRuntime removeShutdownHook this
          _manager = None
        }
      }
    }
  }

  /**
   * {@linkplain FsManager#sync Synchronizes} any
   * {@linkplain #register registered} file system manager.
   * <p>
   * If any exception occurs within the shutdown hook, its stacktrace gets
   * printed to standard error because logging doesn't work in a shutdown
   * hook.
   * 
   * @deprecated Do <em>not</em> call this method explicitly!
   * @see #register
   */
  @deprecated
  override def run() {
    // HC SVNT DRACONES!
    _manager foreach { manager =>
      _manager = None // MUST reset to void calls to cancel() during the sync()!
      try {
        manager sync FsSyncOptions.UMOUNT // may call cancel()!
      } catch {
        // Logging doesn't work in a shutdown hook!
        case ex => ex printStackTrace()
      }
    }
  }
}
