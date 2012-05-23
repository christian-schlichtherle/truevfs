/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.cio

import ScalableContainer._

/**
 * A mixin for containers which persist their entries in a tree.
 * This class is designed to save some heap space with deeply nested directory
 * structures where many entry name segments are duplicates.
 * <p>
 * This mixin is <em>not</em> thread-safe!
 * 
 * @param  <E> the type of the entries in this container.
 * @author Christian Schlichtherle
 */
trait ScalableContainer[E >: Null <: Entry] { this: Container[E] =>

  private implicit def container = this

  protected val splitter = new Splitter('/')

  private[this] val rootNode: Node[E] = new Node(None)

  private var _size: Int = _

  def apply(path: String) = node(path) flatMap (_ entry)

  private def node(path: String): Option[Node[E]] = {
    if (path isEmpty) {
      new Some(rootNode)
    } else {
      val (parentPath, memberName) = splitter(path)
      node(parentPath) flatMap (_(memberName))
    }
  }

  def update(path: String, entry: Option[E]) {
    entry match {
      case Some(e) => require(null ne e); add(path, entry)
      case _       => remove(path)
    }
  }

  def add(path: String, entry: E): Boolean = {
    require(null ne entry)
    add(path, Some(entry)).entry isEmpty
  }

  private def add(path: String, entry: Option[E]): Node[E] = {
    if (path isEmpty) {
      entry foreach (_ => rootNode entry = entry)
      rootNode
    } else {
      val (parentPath, memberName) = splitter(path)
      val parentNode = add(parentPath, None)
      parentNode(memberName) match {
        case Some(memberNode) =>
          entry.foreach(_ => memberNode entry = entry)
          memberNode
        case _ =>
          val memberNode = new Node(entry)
          parentNode(memberName) = Some(memberNode)
          memberNode
      }
    }
  }

  def remove(path: String) = {
    if (path isEmpty) {
      if (0 != rootNode.size)
        throw new IllegalStateException("Node not empty!")
      val rootEntry = rootNode.entry
      rootNode entry = None
      rootEntry
    } else {
      val (parentPath, memberName) = splitter(path)
      node(parentPath) flatMap { parentNode =>
        parentNode(memberName) flatMap { memberNode =>
          // HC SVNT DRACONES!
          if (0 != memberNode.size)
            throw new IllegalStateException("Node not empty!")
          parentNode(memberName) = None
          val oldEntry = memberNode.entry
          memberNode entry = None // update _size conditionally
          oldEntry
        }
      }
    }
  } isDefined

  override def size = _size

  override def iterator =
    collection.JavaConversions.asIterator(rootNode recursiveIterator)

  override def entry(path: String) = apply(path) orNull
} // ScalableContainer

object ScalableContainer {

  private final class Node[E >: Null <: Entry]
  (private[this] var _entry: Option[E])
  (implicit container: ScalableContainer[E])
  extends collection.Iterable[Node[E]] {

    private[this] var _members = collection.SortedMap[String, Node[E]]()

    if (_entry isDefined)
      container._size += 1

    def entry = _entry

    def entry_=(entry: Option[E])(implicit container: ScalableContainer[E]) {
      // HC SVNT DRACONES!
      if (_entry isDefined) {
        if (entry isEmpty)
          container._size -= 1
      } else {
        if (entry isDefined)
          container._size += 1
      }
      _entry = entry
    }

    def apply(name: String) = _members get name

    def update(name: String, node: Option[Node[E]]) {
      node match {
        case Some(node) => _members += new String(name) -> node // don't share sub-strings!
        case _          => _members -= name
      }
    }

    override def iterator = _members.valuesIterator filter (_.entry isDefined)

    def recursiveIterator: Iterator[E] =
      _entry.iterator ++ _members.valuesIterator.flatMap(_ recursiveIterator)
  } // Node
} // ScalableContainer

/*class ScalableEntry[E <: Entry] private(
  node: ScalableContainer.Node[E],
  parent: Option[ScalableContainer.Node[E]])
extends DecoratingEntry(node.entry.get) {

  assert(null eq node.entry.get.getName, "There will be no heap space savings for entries with a non-null name!")

  override getName = {
    
  }
}*/
