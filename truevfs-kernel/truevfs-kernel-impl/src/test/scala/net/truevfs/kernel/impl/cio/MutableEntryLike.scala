/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.impl.cio

import net.truevfs.kernel.cio.Entry.Access._
import net.truevfs.kernel.cio.Entry.Size._
import net.truevfs.kernel.impl.util._

/**
  * @author Christian Schlichtherle
  */
trait MutableEntryLike extends EntryLike {
  type IndexedProperty[-A, B] <: MutableIndexedProperty[A, B]

  final def dataSize_=(value: Long) = size(DATA) = value
  final def storageSize_=(value: Long) = size(STORAGE) = value

  final def createTime_=(value: Long) = time(CREATE) = value
  final def readTime_=(value: Long) = time(READ) = value
  final def writeTime_=(value: Long) = time(WRITE) = value
  final def executeTime_=(value: Long) = time(EXECUTE) = value
}
