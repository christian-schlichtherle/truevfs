/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.truevfs.kernel.impl

import de.schlichtherle.truecommons.shed._
import javax.annotation.concurrent._

/** Indicates that all file system locks need to get released before the
  * operation can get retried.
  *
  * @see    SyncController
  * @author Christian Schlichtherle
  */
@Immutable
private final class NeedsLockRetryException private ()
extends ControlFlowException

private object NeedsLockRetryException {
  import ControlFlowException._

  private[this] val instance = new NeedsLockRetryException

  def apply() = if (isTraceable) new NeedsLockRetryException else instance
}
