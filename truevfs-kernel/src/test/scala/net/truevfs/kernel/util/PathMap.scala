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
final class PathMap[K >: Null <: AnyRef, V]
(implicit converter: Converter[K], private val ordering: Ordering[K])
extends collection.mutable.Map[K, V]
with collection.mutable.MapLike[K, V, PathMap[K, V]] {

  private[this] var _root: Node[K, V] = _
  private var _size: Int = _

  reset()

  implicit private def self = this

  private def reset() { _root = new Node(None); _size = 0}

  override def size = _size

  override def clear() = reset()

  override def empty = new PathMap[K, V]

  override def iterator = _root recursiveEntriesIterator (None)

  override def get(path: K) = node(Option(path)) flatMap (_ value)

  private def node(path: Option[K]): Option[Node[K, V]] = {
    path match {
      case Some(path) =>
        path match {
          case converter(parent, segment) =>
            node(parent) flatMap (_.get(segment))
        }
      case None =>
        Some(_root)
    }
  }

  def list(path: Option[K]) = {
    node(path) map (_.members map {
      case (segment, value) => converter(path, segment) -> value
    })
  }

  override def +=(entry: (K, V)) = {
    add(Option(entry._1), Some(entry._2))
    this
  }

  private def add(path: Option[K], value: Option[V]): Node[K, V] = {
    path match {
      case Some(path) =>
        path match {
          case converter(parent, segment) =>
            add(parent, None) add (segment, value)
        }
      case None =>
        if (value isDefined) _root value = value
        _root
    }
  }

  override def -=(path: K) = {
    remove(Option(path))
    this
  }

  private def remove(path: Option[K]) {
    path match {
      case Some(path) =>
        path match {
          case converter(parent, segment) =>
            node(parent) foreach { node =>
              node remove segment
              if (node isEmpty) remove(parent)
            }
        }
      case None =>
        _root value = None
    }
  }
} // PathMap

object PathMap {

  def apply[K >: Null <: AnyRef, V](converter: Converter[K])
  (implicit ordering: Ordering[K]) = new PathMap[K, V]()(converter, ordering)

  def apply[V](separator: Char): PathMap[String, V] =
    apply(new Splitter(separator))

  private final class Node[K >: Null <: AnyRef, V]
  (private[this] var _value: Option[V])
  (implicit map: PathMap[K, V]) {

    private[this] var _members =
      new collection.immutable.TreeMap[K, Node[K, V]]()(map.ordering)

    if (_value isDefined) map._size += 1

    def value = _value
    def value_=(value: Option[V])(implicit map: PathMap[K, V]) {
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

    def isEmpty = _value.isEmpty && 0 == _members.size

    final def get(segment: K) = _members get segment

    final def add(segment: K, value: Option[V])(implicit map: PathMap[K, V]) = {
      _members get segment match {
        case Some(node) =>
          if (value isDefined) node value = value
          node
        case None =>
          val node = new Node[K, V](value)
          _members += segment -> node
          node
      }
    }

    final def remove(segment: K)(implicit map: PathMap[K, V]) {
      _members get segment map { node =>
        node value = None
        if (node isEmpty) _members -= segment
      }
    }

    final def recursiveEntriesIterator(path: Option[K])
    (implicit converter: Converter[K]): Iterator[(K, V)] = {
      entry(path).iterator ++ _members.iterator.flatMap {
        case (segment, node) =>          
          node recursiveEntriesIterator Some(converter(path, segment))
      }
    }

    private def entry(path: Option[K]) = _value map (path.orNull -> _)

    def members = _members flatMap {
      case (segment, node) => node.value map (segment -> _)
    }
  } // Node

  trait Converter[K] extends ((Option[K], K) => K) {
    /** The injection method. */
    override def apply(parent: Option[K], segment: K): K

    /**
     * The extraction method.
     * Note that the second element of the tuple should not share any memory
     * with the given path - otherwise you will not achieve any heap space
     * savings!
     */
    def unapply(path: K): Some[(Option[K], K)]
  } // Converter

  final case class Splitter(separator: Char) extends Converter[String] {
    override def apply(parent: Option[String], segment: String) = {
      parent match {
        case Some(parent) => parent + segment
        case None => segment
      }
    }

    override def unapply(path: String) = {
      val i = path.lastIndexOf(separator)
      if (0 <= i) {
        // Don't share sub-strings with the PathMap!
        Some(Some(path substring (0, i)) -> new String(path substring i))
      } else {
        // There's no point in copying here!
        Some(None, path)
      } 
    }
  } // PathConverter
} // PathMap
