/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl.cio

import net.java.truevfs.kernel.spec.cio._
import net.java.truevfs.kernel.impl.util.FileSystem

/**
  * @author Christian Schlichtherle
  */
class FileSystemContainer[E >: Null <: Entry] extends Container[E] {

  private[this] val map = FileSystem[E]('/')

  final override def size = map size

  final override def iterator = {
    import collection.JavaConversions._
    map valuesIterator
  }

  final override def entry(name: String) = map get name orNull

  def link(name: String, entry: E) = map link (name, entry)

  def unlink(name: String) = map unlink name
}
