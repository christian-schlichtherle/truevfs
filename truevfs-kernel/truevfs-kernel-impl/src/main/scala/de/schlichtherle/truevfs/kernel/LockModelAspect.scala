/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truevfs.kernel

import javax.annotation.concurrent._

/** A mixin which implements some features of its associated
  * [[de.schlichtherle.truevfs.kernel.LockModel]].
  *
  * @author Christian Schlichtherle
  */
@ThreadSafe
private trait LockModelAspect
extends GenModelAspect[LockModel] with LockModelLike {
  final def lock = model.lock
}
