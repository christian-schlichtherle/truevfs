/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.cio

import net.truevfs.kernel.cio.Entry.Size._
import net.truevfs.kernel.cio.Entry.Access._
import net.truevfs.kernel.cio.Entry._
import net.truevfs.kernel.cio._
import net.truevfs.kernel._

trait EntryLike {
  type IndexedProperty[-A, B] <: A => B

  def name: String

  def size: IndexedProperty[Size, Long]
  final def dataSize = size(DATA)
  final def storageSize = size(STORAGE)

  def time: IndexedProperty[Access, Long]
  final def createTime = time(CREATE)
  final def readTime = time(READ)
  final def writeTime = time(WRITE)
  final def executeTime = time(EXECUTE)

  def permission(tµpe: Access): IndexedProperty[Entity, Option[Boolean]]
  final def createPermission = permission(CREATE)
  final def readPermission = permission(READ)
  final def writePermission = permission(WRITE)
  final def executePermission = permission(EXECUTE)
}

trait MutableIndexedProperty[-A, B] extends (A => B) {
  def update(index: A, value: B)
}

trait MutableEntryLike extends EntryLike {
  type IndexedProperty[-A, B] <: MutableIndexedProperty[A, B]

  final def dataSize_=(value: Long) = size(DATA) = value
  final def storageSize_=(value: Long) = size(STORAGE) = value

  final def createTime_=(value: Long) = time(CREATE) = value
  final def readTime_=(value: Long) = time(READ) = value
  final def writeTime_=(value: Long) = time(WRITE) = value
  final def executeTime_=(value: Long) = time(EXECUTE) = value
}

trait EntryAspect[E <: Entry] extends EntryLike {
  def entry: E
  final def name = entry.getName
}

class EntryOps[E <: Entry](val entry: E) extends EntryAspect[E] { 
  type IndexedProperty[-A, B] = A => B

  final def size = entry.getSize(_)
  final def time = entry.getTime(_)
  final def permission(tµpe: Access) = { entity =>
    val p = entry.isPermitted(tµpe, entity)
    if (null ne p) Option(p) else None
  }
}

object EntryOps {
  implicit def apply[E <: Entry](entry: E) = new EntryOps(entry)
}

class MutableEntryOps[E <: MutableEntry](val entry: E) extends EntryAspect[E] {
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

object MutableEntryOps {
  implicit def apply[E <: MutableEntry](entry: E) = new MutableEntryOps(entry)
}

class ArchiveEntryOps[E <: FsArchiveEntry](entry: E)
extends MutableEntryOps[E](entry) {
  def tµpe: Type = entry.getType
}

object ArchiveEntryOps {
  implicit def apply[E <: FsArchiveEntry](entry: E) = new ArchiveEntryOps(entry)
}
