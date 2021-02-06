/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.it.cio

import global.namespace.truevfs.comp.cio._
import global.namespace.truevfs.it.util._

import java.util
import java.util.Optional
import scala.jdk.CollectionConverters._

/**
 * @author Christian Schlichtherle
 */
class FileSystemContainer[E >: Null <: Entry] extends Container[E] {

  private[this] var map = FileSystem[E]('/')

  final override def entries: util.Collection[E] = map.asJava.values

  final override def entry(name: String): Optional[E] = Optional.ofNullable(map.get(name).orNull)

  def link(name: String, entry: E): FileSystem.Node[String, E] = map link(name, entry)

  def unlink(name: String): Unit = map unlink name

  override def close(): Unit = {
    map = null
  }
}
