/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl.cio

import language.higherKinds
import net.java.truecommons.cio.Entry._
import net.java.truecommons.cio.Entry.Access._
import net.java.truecommons.cio.Entry.Size._

/**
  * @author Christian Schlichtherle
  */
trait EntryLike {

  type IndexedProperty[-A, B] <: A => B

  def name: String

  def size: IndexedProperty[Size, Long]

  final def dataSize: Long = size(DATA)

  final def storageSize: Long = size(STORAGE)

  def time: IndexedProperty[Access, Long]

  final def createTime: Long = time(CREATE)

  final def readTime: Long = time(READ)

  final def writeTime: Long = time(WRITE)

  final def executeTime: Long = time(EXECUTE)

  def permission(tµpe: Access): IndexedProperty[Entity, Option[Boolean]]

  final def createPermission: IndexedProperty[Entity, Option[Boolean]] = permission(CREATE)

  final def readPermission: IndexedProperty[Entity, Option[Boolean]] = permission(READ)

  final def writePermission: IndexedProperty[Entity, Option[Boolean]] = permission(WRITE)

  final def executePermission: IndexedProperty[Entity, Option[Boolean]] = permission(EXECUTE)

  final def deletePermission: IndexedProperty[Entity, Option[Boolean]] = permission(DELETE)
}
