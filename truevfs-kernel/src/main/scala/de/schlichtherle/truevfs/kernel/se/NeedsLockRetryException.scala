/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.schlichtherle.truevfs.kernel.se

/**
 * Indicates that all file system locks need to get released before the
 * operation can get retried.
 *
 * @see    SyncController
 * @author Christian Schlichtherle
 */
private final class NeedsLockRetryException private()
extends ControlFlowException

private object NeedsLockRetryException {
  import ControlFlowException._

  private[this] val instance = new NeedsLockRetryException

  def apply() = if (traceable) new NeedsLockRetryException else instance
}
