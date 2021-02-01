/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.it.cio

import global.namespace.truevfs.comp.cio.Entry._
import global.namespace.truevfs.comp.cio._

import scala.language.implicitConversions

/**
  * @author Christian Schlichtherle
  */
trait EntryAspect[E <: Entry] extends GenEntryAspect[E] with EntryLike {

  type IndexedProperty[-A, B] = A => B

  final def size: Size => Long = entry.getSize(_)

  final def time: Access => Long = entry.getTime(_)

  final def permission(tµpe: Access): Entity => Option[Boolean] = {
    entity => Option(entry.isPermitted(tµpe, entity).orElse(null))
  }
}

/**
  * @author Christian Schlichtherle
  */
object EntryAspect {

  implicit def apply[E <: Entry](e: E): EntryAspect[E] = new EntryAspect[E] {

    def entry: E = e
  }
}
