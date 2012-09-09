/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl

import javax.annotation.concurrent._

/** A mixin which provides some features of its associated
  * [[net.java.truevfs.kernel.impl.LockModel]].
  *
  * @author Christian Schlichtherle
  */
@ThreadSafe
private trait LockModelAspect
extends ModelAspect[LockModel] with ReentrantReadWriteLockAspect {
  final override def lock = model.lock
}
