/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl.cio

import collection.JavaConverters._
import net.java.truecommons.cio._
import net.java.truevfs.kernel.impl.util._

/**
  * @author Christian Schlichtherle
  */
class FileSystemContainer[E >: Null <: Entry] extends Container[E] {

  private[this] var map = FileSystem[E]('/')

  final override def size = map.size

  final override def iterator = map.valuesIterator.asJava

  final override def entry(name: String) = (map get name).orNull

  def link(name: String, entry: E) = map link (name, entry)

  def unlink(name: String) = map unlink name
  
  override def close() { map = null }
}
