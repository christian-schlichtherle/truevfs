/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl.cio

import net.java.truecommons.cio._
import net.java.truevfs.kernel.impl.util._

import java.util
import scala.jdk.CollectionConverters._

/**
  * @author Christian Schlichtherle
  */
class FileSystemContainer[E >: Null <: Entry] extends Container[E] {

  private[this] var map = FileSystem[E]('/')

  final override def size: Int = map.size

  final override def iterator: util.Iterator[E] = map.valuesIterator.asJava

  final override def entry(name: String): E = (map get name).orNull

  def link(name: String, entry: E): FileSystem.Node[String, E] = map link (name, entry)

  def unlink(name: String): Unit = map unlink name
  
  override def close(): Unit = { map = null }
}
