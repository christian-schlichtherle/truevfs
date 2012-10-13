/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.pace

import javax.annotation.concurrent._
import net.java.truecommons.shed._
import net.java.truevfs.kernel.spec._

/**
  * Calls back the given pace manager before and after each file system
  * operation in order to sync the least recently accessed file systems which
  * exceed the maximum number of mounted file systems and then register itself
  * as the most recently accessed file system.
  * 
  * @author Christian Schlichtherle
  */
@ThreadSafe
private class PaceController(manager: PaceManager, controller: FsController)
extends AspectController(controller) {

  override def apply[V](operation: () => V) = {
    manager preAccess controller
    val result = operation()
    manager postAccess controller
    result
  }

  override def sync(options: BitField[FsSyncOption]) = controller sync options // skip apply!
}
