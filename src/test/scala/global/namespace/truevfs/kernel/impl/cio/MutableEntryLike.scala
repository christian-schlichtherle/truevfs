/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.kernel.impl.cio

import language.higherKinds
import global.namespace.truevfs.comp.cio.Entry.Access._
import global.namespace.truevfs.comp.cio.Entry.Size._
import global.namespace.truevfs.kernel.impl.util._

/**
  * @author Christian Schlichtherle
  */
trait MutableEntryLike extends EntryLike {

  type IndexedProperty[-A, B] <: MutableIndexedProperty[A, B]

  final def dataSize_=(value: Long): Unit = size(DATA) = value

  final def storageSize_=(value: Long): Unit = size(STORAGE) = value

  final def createTime_=(value: Long): Unit = time(CREATE) = value

  final def readTime_=(value: Long): Unit = time(READ) = value

  final def writeTime_=(value: Long): Unit = time(WRITE) = value

  final def executeTime_=(value: Long): Unit = time(EXECUTE) = value
}
