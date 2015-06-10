/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.pacemaker

import net.java.truecommons.shed.OnTryFinally._
import net.java.truecommons.shed._
import net.java.truevfs.kernel.spec._

/** Calls back the given pace manager after each file system operation in order
  * to register itself as the most recently accessed file system and unmount the
  * least recently accessed file systems which exceed the maximum number of
  * mounted file systems.
  *
  * @author Christian Schlichtherle
  */
private class PaceController(manager: PaceManager, controller: FsController)
extends AspectController(controller) {

  override def apply[V](operation: () => V) =
    onTry {
      operation()
    } onFinally {
      manager recordAccess getMountPoint
    }

  override def sync(options: BitField[FsSyncOption]) { controller sync options } // skip apply!
}
