/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.cio

import ScalableContainer._

/**
 * A mixin for containers which persist their entries in a tree.
 * This saves heap space with deeply nested entry hierarchies where many
 * entry name segments are duplicates.
 * <p>
 * This mixin is <em>not</em> thread-safe!
 * 
 * @param  <E> the type of the entries in this container.
 * @author Christian Schlichtherle
 */
import net.truevfs.kernel.util.PathSplitter

trait ScalableContainer[E >: Null <: Entry] { this: Container[E] =>

  private implicit def container = this

  protected val rootName: String = null
  protected val separatorChar = '/'
  protected lazy val splitter = new PathSplitter(separatorChar, false)

  private[this] val root: Node[E] = new Node(None)

  private var _size: Int = _

  private def split(path: String) = {
    val s = splitter
    s split path
    (s getParentPath, s getMemberName)
  }

  def apply(path: String) = entry(path)

  def update(path: String, entry: E) {
    if (null ne entry) add(path, entry)
    else remove(path)
  }

  def add(path: String, entry: E): Boolean = {
    require(null ne entry)
    add0(path, new Some(entry)).entry isEmpty
  }

  private def add0(path: String, entry: Option[E]): Node[E] = {
    if (path == rootName) {
      entry foreach (_ => root entry = entry)
      root
    } else {
      val (parentPath, memberName) = split(path)
      val parentNode = add0(parentPath, None)
      parentNode(memberName) match {
        case Some(memberNode) =>
          entry.foreach(_ => memberNode entry = entry)
          memberNode
        case _ =>
          val memberNode = new Node(entry)
          parentNode(memberName) = new Some(memberNode)
          memberNode
      }
    }
  }

  def remove(path: String) = {
    if (path == rootName) {
      if (0 != root.size)
        throw new IllegalStateException("Node not empty!")
      val rootEntry = root.entry
      root entry = None
      rootEntry
    } else {
      val (parentPath, memberName) = split(path)
      node(parentPath) flatMap { parentNode =>
        parentNode(memberName) flatMap { memberNode =>
          // HC SVNT DRACONES!
          if (0 != memberNode.size)
            throw new IllegalStateException("Node not empty!")
          parentNode(memberName) = None
          val oldEntry = memberNode.entry
          memberNode.entry = None // update _size conditionally
          oldEntry
        }
      }
    }
  } isDefined

  private def node(path: String): Option[Node[E]] = {
    if (path == rootName) {
      new Some(root)
    } else {
      val (parentPath, memberName) = split(path)
      node(parentPath) flatMap (_(memberName))
    }
  }

  override def size = _size

  override def iterator =
    collection.JavaConversions.asIterator(root allEntries)

  override def entry(path: String) = node(path) flatMap (_ entry) orNull
} // ScalableContainer

private object ScalableContainer {

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

    override def size = _members size

    override def iterator: Iterator[Node[E]] = _members valuesIterator

    def allEntries: Iterator[E] = _entry.iterator ++ _members.values.flatMap(_ allEntries)

    def apply(name: String) = _members get name

    def update(name: String, node: Option[Node[E]]) {
      node match {
        case Some(node) => _members += new String(name) -> node // don't share sub-strings!
        case _ => _members -= name
      }
    }
  } // Node
} // ScalableContainer
