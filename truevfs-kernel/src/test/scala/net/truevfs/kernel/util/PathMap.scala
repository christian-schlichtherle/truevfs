/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.util

/**
 * A mutable map for path names to any reference values.
 * This class is designed to save some heap space if the path names address
 * deeply nested directory structures where many path name segments can get
 * shared between entries.
 * Note that you need to make sure that the path names are eligible for garbage
 * collection in order to achieve these heap space savings!
 * <p>
 * This class is <em>not</em> thread-safe!
 * <p>
 * Implementation note: This class <em>must not</em> share any strings with its
 * clients in order to achieve the heap space savings!
 * 
 * @param  <V> the type of the values in this map.
 * @author Christian Schlichtherle
 */
final class PathMap[V](implicit private[this] val separator: Char = '/')
extends collection.mutable.Map[String, V]
with collection.mutable.MapLike[String, V, PathMap[V]] {
  import PathMap._

  private[this] implicit def container = this

  private[this] val splitter = new Splitter(separator)
  private[this] val rootNode = new Node[V](None)

  private var _size: Int = _

  override def size = _size

  override def empty = new PathMap[V]

  override def +=(entry: (String, V)) = {
    val (path, value) = entry
    add(path, Some(value))
    this
  }

  private def add(path: String, value: Option[V]): Node[V] = {
    if (path isEmpty) {
      value foreach (_ => rootNode value = value)
      rootNode
    } else {
      val (parentPath, memberName) = splitter(path)
      add(parentPath, None).add(memberName, value)
    }
  }

  override def -=(path: String) = {
    if (path isEmpty) {
      rootNode value = None
    } else {
      val (parentPath, memberName) = splitter(path)
      node(parentPath) foreach (_.remove(memberName))
    }
    this
  }

  override def get(path: String) = node(path) flatMap (_ value)

  private def node(path: String): Option[Node[V]] = {
    if (path isEmpty) {
      new Some(rootNode)
    } else {
      val (parentPath, memberName) = splitter(path)
      node(parentPath) flatMap (_.get(memberName))
    }
  }

  override def iterator = rootNode recursiveEntriesIterator ""
} // PathMap

private object PathMap {

  private final class Node[V](private[this] var _value: Option[V])
  (implicit map: PathMap[V]) {

    private var _members = new collection.immutable.TreeMap[String, Node[V]]

    if (_value isDefined) map._size += 1

    def value = _value

    def value_=(value: Option[V])(implicit map: PathMap[V]) {
      // HC SVNT DRACONES!
      if (_value isDefined) {
        if (value isEmpty)
          map._size -= 1
      } else {
        if (value isDefined)
          map._size += 1
      }
      _value = value
    }

    def get(memberName: String) = _members get memberName

    def add(memberName: String, value: Option[V])(implicit map: PathMap[V]) = {
      _members get memberName match {
        case Some(node) =>
          value foreach (_ => node value = value)
          node
        case None =>
          val node = new Node(value)
          _members += new String(memberName) -> node // don't share strings with clients!
          node
      }
    }

    def remove(memberName: String)(implicit map: PathMap[V]) {
      _members get memberName foreach { node =>
        node value = None
        if (node._members isEmpty) _members -= memberName
      }
    }

    def recursiveEntriesIterator(path: String)(implicit separator: Char)
    : Iterator[(String, V)] = {
      val emptyPath = path.isEmpty
      entry(path).iterator ++ _members.iterator.flatMap {
        case (memberName, memberNode) =>
        val memberPath = {
          if (emptyPath) memberName
          else path + separator + memberName
        }
        memberNode recursiveEntriesIterator memberPath
      }
    }

    private def entry(path: String) = _value map (path -> _)
  } // Node

  private final class Splitter(separator: Char)
  extends PathSplitter(separator, false) {
    def apply(path: String) = { split(path); (getParentPath, getMemberName) }
    override def getParentPath = Option(super.getParentPath) getOrElse ""
  } // Splitter
} // PathMap
