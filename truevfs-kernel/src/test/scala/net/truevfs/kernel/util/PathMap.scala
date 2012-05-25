/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.util

import PathMap._

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
final class PathMap[V]
(implicit private[this] val Path: PathMap.Converter[String] = new PathConverter('/'))
extends collection.mutable.Map[String, V]
with collection.mutable.MapLike[String, V, PathMap[V]] {

  private[this] implicit def container = this

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
    path match {
      case Path(Some(parentPath), memberName) =>
        add(parentPath, None).add(memberName, value)
      case Path(None, null) =>
        value foreach (_ => rootNode value = value)
        rootNode
      case Path(None, memberName) =>
        rootNode.add(memberName, value)
    }
  }

  override def -=(path: String) = {
    path match {
      case Path(Some(parentPath), memberName) =>
        node(parentPath) foreach (_.remove(memberName))
      case Path(None, null) =>
        rootNode value = None
      case Path(None, memberName) =>
        rootNode.remove(memberName)
    }
    this
  }

  override def get(path: String) = node(path) flatMap (_ value)

  private def node(path: String): Option[Node[V]] = {
    path match {
      case Path(Some(parentPath), memberName) =>
        node(parentPath) flatMap (_.get(memberName))
      case Path(None, null) =>
        Some(rootNode)
      case Path(None, memberName) =>
        rootNode.get(memberName)
    }
  }

  override def iterator = rootNode recursiveEntriesIterator None
} // PathMap

object PathMap {

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

    def recursiveEntriesIterator(path: Option[String])(implicit Path: Converter[String])
    : Iterator[(String, V)] = {
      entry(path).iterator ++ _members.iterator.flatMap {
        case (memberName, memberNode) =>          
          memberNode recursiveEntriesIterator Some(Path(path, memberName))
      }
    }

    private def entry(path: Option[String]) = _value map (path.orNull -> _)
  } // Node

  trait Converter[V] {
    def apply(parent: Option[V], member: V): V
    def unapply(path: V): Some[(Option[V], V)]
  } // Converter

  final case class PathConverter(separator: Char)
  extends PathSplitter(separator, false) with Converter[String] {
    def apply(parentPath: Option[String], memberName: String) = {
      parentPath match {
        case Some(parent) => parent + separator + memberName
        case None => memberName
      }
    }

    def unapply(path: String) = {
      split(path)
      Some(Option(super.getParentPath), getMemberName)
    }
  } // PathConverter
} // PathMap
