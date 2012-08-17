/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.pacemanager

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
private final class PaceController(m: PaceManager, c: FsController)
extends AspectController(c) {

  override def apply[V](operation: => V) = {
    m retain c
    val r = operation
    m accessed c
    r
  }

  override def sync(options: BitField[FsSyncOption]) = c sync options // skip apply!
}
