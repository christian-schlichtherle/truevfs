package global.namespace.truevfs.it.cio

import global.namespace.truevfs.comp.cio.Entry.Access.{CREATE, EXECUTE, READ, WRITE}
import global.namespace.truevfs.comp.cio.Entry.Size.{DATA, STORAGE}
import global.namespace.truevfs.it.util.MutableIndexedProperty

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
