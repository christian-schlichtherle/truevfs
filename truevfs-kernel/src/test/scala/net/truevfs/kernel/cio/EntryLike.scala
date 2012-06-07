/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.cio

import net.truevfs.kernel.cio.Entry._
import net.truevfs.kernel.cio.Entry.Access._
import net.truevfs.kernel.cio.Entry.Size._

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
