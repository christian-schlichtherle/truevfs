/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl.cio

import net.java.truecommons.cio._
import net.java.truecommons.cio.Entry._
import net.java.truevfs.kernel.impl.util._

/**
  * @author Christian Schlichtherle
  */
trait MutableEntryAspect[E <: MutableEntry]
extends GenEntryAspect[E] with MutableEntryLike {
  type IndexedProperty[-A, B] = MutableIndexedProperty[A, B]

  final def size = new MutableIndexedProperty[Size, Long] {
    def apply(tµpe: Size) = entry.getSize(tµpe)
    def update(tµpe: Size, value: Long) { entry.setSize(tµpe, value) }
  }

  final def time = new MutableIndexedProperty[Access, Long] {
    def apply(tµpe: Access) = entry.getTime(tµpe)
    def update(tµpe: Access, value: Long) { entry.setTime(tµpe, value) }
  }

  final def permission(tµpe: Access) = {
    new MutableIndexedProperty[Entity, Option[Boolean]] {
      def apply(entity: Entity) = {
        val p = entry.isPermitted(tµpe, entity)
        if (null ne p) Option(p) else None
      }

      def update(entity: Entity, value: Option[Boolean]) {
        entry.setPermitted(tµpe, entity, value.map(Boolean.box(_)).orNull)
      }
    }
  }
}

/**
  * @author Christian Schlichtherle
  */
object MutableEntryAspect {
  implicit def apply[E <: MutableEntry](e: E) =
    new MutableEntryAspect[E] { def entry = e }
}
