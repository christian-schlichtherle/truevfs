/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truevfs.kernel.se

import net.truevfs.kernel._

/**
 * A file system controller mixin which requires a {@link LockModel} so that
 * it can forward calls to its additional methods to this model for the
 * convenience of sub-classes.
 *
 * @see    LockModel
 * @author Christian Schlichtherle
 */
private trait LockModelController extends FsController[LockModel] {

  def readLock = getModel.readLock

  final def isReadLockedByCurrentThread =
    getModel.isReadLockedByCurrentThread

  def writeLock = getModel.writeLock

  final def isWriteLockedByCurrentThread =
    getModel.isWriteLockedByCurrentThread

  final def checkWriteLockedByCurrentThread =
    getModel.checkWriteLockedByCurrentThread
}
