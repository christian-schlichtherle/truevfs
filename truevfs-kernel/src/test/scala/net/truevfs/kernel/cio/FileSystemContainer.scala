/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.cio

import net.truevfs.kernel.util.FileSystem

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
