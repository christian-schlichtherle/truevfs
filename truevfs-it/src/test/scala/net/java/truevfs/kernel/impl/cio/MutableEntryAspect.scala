/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl.cio

import language.implicitConversions
import net.java.truecommons.cio._
import net.java.truecommons.cio.Entry._
import net.java.truevfs.kernel.impl.util._

/**
  * @author Christian Schlichtherle
  */
trait MutableEntryAspect[E <: MutableEntry] extends GenEntryAspect[E] with MutableEntryLike {

  type IndexedProperty[-A, B] = MutableIndexedProperty[A, B]

  final def size: MutableIndexedProperty[Size, Long] = {
    new MutableIndexedProperty[Size, Long] {

      def apply(tµpe: Size): Long = entry.getSize(tµpe)

      def update(tµpe: Size, value: Long): Unit = { entry.setSize(tµpe, value) }
    }
  }

  final def time: MutableIndexedProperty[Access, Long] = {
    new MutableIndexedProperty[Access, Long] {

      def apply(tµpe: Access): Long = entry.getTime(tµpe)

      def update(tµpe: Access, value: Long): Unit = { entry.setTime(tµpe, value) }
    }
  }

  final def permission(tµpe: Access): MutableIndexedProperty[Entity, Option[Boolean]] = {
    new MutableIndexedProperty[Entity, Option[Boolean]] {

      def apply(entity: Entity): Option[Boolean] = {
        val p = entry.isPermitted(tµpe, entity)
        if (null ne p) {
          Option(p)
        } else {
          None
        }
      }

      override def update(entity: Entity, value: Option[Boolean]): Unit = {
        entry.setPermitted(tµpe, entity, value.map(Boolean.box).orNull)
      }
    }
  }
}

/**
  * @author Christian Schlichtherle
  */
object MutableEntryAspect {

  implicit def apply[E <: MutableEntry](e: E): MutableEntryAspect[E] = {
    new MutableEntryAspect[E] {
      def entry: E = e
    }
  }
}
