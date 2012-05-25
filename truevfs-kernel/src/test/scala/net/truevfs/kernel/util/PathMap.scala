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
final class PathMap[V <: AnyRef]
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
      case Path(Some(parentPath), memberSegment) =>
        add(parentPath, None).add(memberSegment, value)
      case Path(None, memberSegment) =>
        rootNode.add(memberSegment, value)
    }
  }

  override def -=(path: String) = {
    path match {
      case Path(Some(parentPath), memberSegment) =>
        node(parentPath) foreach (_.remove(memberSegment))
      case Path(None, memberSegment) =>
        rootNode.remove(memberSegment)
    }
    this
  }

  override def get(path: String) = node(path) flatMap (_ value)

  private def node(path: String): Option[Node[V]] = {
    path match {
      case Path(Some(parentPath), memberSegment) =>
        node(parentPath) flatMap (_.get(memberSegment))
      case Path(None, memberSegment) =>
        rootNode.get(memberSegment)
    }
  }

  override def iterator = rootNode recursiveEntriesIterator None
} // PathMap

object PathMap {

  private final class Node[V <: AnyRef](private[this] var _value: Option[V])
  (implicit map: PathMap[V]) {

    private[this] var _members = new collection.immutable.TreeMap[String, Node[V]]

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

    def add(memberSegment: String, value: Option[V])(implicit map: PathMap[V]) = {
      _members get memberSegment match {
        case Some(node) =>
          value foreach (_ => node value = value)
          node
        case None =>
          val node = new Node(value)
          _members += memberSegment -> node
          node
      }
    }

    def remove(memberSegment: String)(implicit map: PathMap[V]) {
      _members get memberSegment foreach { node =>
        node value = None
        if (0 == node.size) _members -= memberSegment
      }
    }

    def size = _members size

    def recursiveEntriesIterator(path: Option[String])(implicit Path: Converter[String])
    : Iterator[(String, V)] = {
      entry(path).iterator ++ _members.iterator.flatMap {
        case (memberSegment, memberNode) =>          
          memberNode recursiveEntriesIterator Some(Path(path, memberSegment))
      }
    }

    private def entry(path: Option[String]) = value map (path.get -> _)
  } // Node

  trait Converter[V <: AnyRef] extends ((Option[V], V) => V) {
    /** The injection method. */
    def apply(parentPath: Option[V], memberSegment: V): V

    /**
     * The extraction method.
     * Note that the second element of the tuple should not share any memory
     * with the given path - otherwise you will not achieve any heap space
     * savings!
     */
    def unapply(path: V): Option[(Option[V], V)]
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
      Some(Option(super.getParentPath), new String(getMemberName)) // don't share strings with the PathMap!
    }
  } // PathConverter
} // PathMap
