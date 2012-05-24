/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.cio

import TreeContainer._

/**
 * A container which persists its entries in a tree.
 * This class is designed to save some heap space with deeply nested directory
 * structures where many entry name segments are duplicates.
 * <p>
 * This mixin is <em>not</em> thread-safe!
 * 
 * @param  <E> the type of the entries in this container.
 * @author Christian Schlichtherle
 */
class TreeContainer[E >: Null <: Entry](splitter: Splitter = new Splitter('/'))
extends Container[E] {

  private[this] val tree = new Tree[E](splitter)

  def apply(path: String) = tree.apply(path)

  def update(path: String, entry: Option[E]) = tree.update(path, entry)

  override def size = tree size
  
  override def iterator = collection.JavaConversions.asIterator(tree iterator)

  override def entry(path: String) = tree(path) orNull
} // TreeContainer
