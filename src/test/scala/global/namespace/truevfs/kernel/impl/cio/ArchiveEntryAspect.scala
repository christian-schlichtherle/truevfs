/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.kernel.impl.cio

import language.implicitConversions
import global.namespace.truevfs.kernel.spec._
import global.namespace.truevfs.comp.cio.Entry._

/**
  * @author Christian Schlichtherle
  */
trait ArchiveEntryAspect[E <: FsArchiveEntry] extends MutableEntryAspect[E] {

  def tµpe: Type = entry.getType
}

/**
  * @author Christian Schlichtherle
  */
object ArchiveEntryAspect {

  implicit def apply[E <: FsArchiveEntry](e: E): ArchiveEntryAspect[E] = new ArchiveEntryAspect[E] {

    def entry: E = e
  }
}
