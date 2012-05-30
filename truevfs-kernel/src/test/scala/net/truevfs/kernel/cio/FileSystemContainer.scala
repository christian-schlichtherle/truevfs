/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.cio

import net.truevfs.kernel.util.FileSystem
import PathMapContainer._

class FileSystemContainer[E >: Null <: Entry] extends Container[Node[E]] {

  private[this] val map = FileSystem[Node[E]]('/')

  final override def size = map size

  final override def iterator = {
    import collection.JavaConversions._
    map valuesIterator
  }

  final override def entry(name: String) = map get(name) orNull

  protected def newNode(name: => String, entry: E) =
    new Node(name, entry)

  def add(name: String, entry: E) = {
    val composition = map composition
    val Some((parent, segment)) = composition unapply name
    val node = newNode(composition(parent, segment), entry)
    map += name -> node
    node
  }

  def remove(name: String) {
    map -= name
  }
}

object PathMapContainer {
  class Node[E <: Entry](name: => String, entry: E)
  extends DecoratingEntry(entry) {
    override def getName = name
  }
}
