/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl

import java.util.concurrent.locks._
import javax.annotation.concurrent._
import net.java.truevfs.kernel.spec._

/** A file system model which supports multiple concurrent reader threads.
  *
  * @see    LockController
  * @author Christian Schlichtherle
  */
@ThreadSafe
private final class LockModel(model: FsModel)
extends FsDecoratingModel(model) with ReentrantReadWriteLockAspect {
  val lock = new ReentrantReadWriteLock
}