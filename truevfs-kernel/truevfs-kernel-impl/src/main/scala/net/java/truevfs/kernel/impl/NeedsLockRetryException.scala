/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.java.truevfs.kernel.impl

import net.java.truecommons.shed._

/** Indicates that all file system locks need to get released before the
  * operation can get retried.
  *
  * @see    SyncController
  * @author Christian Schlichtherle
  */
private final class NeedsLockRetryException private ()
extends ControlFlowException(false) with Immutable

private object NeedsLockRetryException {
  import ControlFlowException._

  private[this] val instance = new NeedsLockRetryException

  def apply() = if (isTraceable) new NeedsLockRetryException else instance
}
