/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truevfs.kernel.se

import net.truevfs.kernel._

private abstract class AbstractController[M <: FsModel](val model: M)
extends FsController[M] {

  final override def getModel(): M = model
} // AbstractController
