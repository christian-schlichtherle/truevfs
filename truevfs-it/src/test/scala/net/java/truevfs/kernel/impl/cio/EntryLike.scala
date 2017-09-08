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
  final def dataSize = size(DATA)
  final def storageSize = size(STORAGE)

  def time: IndexedProperty[Access, Long]
  final def createTime = time(CREATE)
  final def readTime = time(READ)
  final def writeTime = time(WRITE)
  final def executeTime = time(EXECUTE)

  def permission(tµpe: Access): IndexedProperty[Entity, Option[Boolean]]
  final def createPermission = permission(CREATE)
  //final def overwritePermission = permission(CREATE)
  final def readPermission = permission(READ)
  final def writePermission = permission(WRITE)
  final def executePermission = permission(EXECUTE)
  final def deletePermission = permission(DELETE)
}
