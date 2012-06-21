/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.impl.cio

import net.truevfs.kernel._
import net.truevfs.kernel.cio.Entry._

/**
  * @author Christian Schlichtherle
  */
trait ArchiveEntryAspect[E <: FsArchiveEntry]
extends MutableEntryAspect[E] {
  def tµpe = entry.getType
}

/**
  * @author Christian Schlichtherle
  */
object ArchiveEntryAspect {
  implicit def apply[E <: FsArchiveEntry](e: E) =
    new ArchiveEntryAspect[E] { def entry = e }
}
