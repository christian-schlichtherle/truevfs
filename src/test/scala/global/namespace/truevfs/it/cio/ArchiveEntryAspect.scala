/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.it.cio

import global.namespace.truevfs.comp.cio.Entry._
import global.namespace.truevfs.kernel.api._

import scala.language.implicitConversions

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
