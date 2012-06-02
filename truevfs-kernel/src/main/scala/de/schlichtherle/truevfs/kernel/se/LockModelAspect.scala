/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truevfs.kernel.se

import net.truevfs.kernel._

/**
 * A mixin which defines some methods on its associated {@link LockModel}.
 *
 * @see    LockModel
 * @author Christian Schlichtherle
 */
private trait LockModelAspect extends ModelAspect[LockModel] {

  final val readLock = model readLock
  final def readLockedByCurrentThread = model isReadLockedByCurrentThread

  final val writeLock = model writeLock
  final def writeLockedByCurrentThread = model isWriteLockedByCurrentThread
  final def checkWriteLockedByCurrentThread = model checkWriteLockedByCurrentThread
}
