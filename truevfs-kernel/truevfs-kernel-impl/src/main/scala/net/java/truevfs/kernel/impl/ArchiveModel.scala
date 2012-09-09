/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl

import java.util.concurrent.locks._
import net.java.truevfs.kernel.spec._

/**
  * @author Christian Schlichtherle
  */
private abstract class ArchiveModel[E <: FsArchiveEntry]
(val driver: FsArchiveDriver[E], model: FsModel)
extends FsDecoratingModel(model) with ReentrantReadWriteLockAspect {

  final override val lock = new ReentrantReadWriteLock

  /** Composes the node path from the mountpoint of this model and the given
    * node name.
    * 
    * @param name the node name.
    */
  final def path(name: FsNodeName) = new FsNodePath(getMountPoint, name)

  def touch(options: AccessOptions)
}
