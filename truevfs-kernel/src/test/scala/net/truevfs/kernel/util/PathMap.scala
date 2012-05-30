/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.util

import collection._
import PathMap._

/**
 * A mutable map with decomposable keys.
 * The standard use case is for mapping path name strings which can get
 * decomposed into segments by splitting them with a separator character such
 * as {@code '/'}.
 * Using this map will then help to save some heap space if the path names
 * address deeply nested directory structures where many segments can get
 * shared between mapped values <em>and</em> no external references to the path
 * names are held!
 * <p>
 * This class supports both {@code null} keys and values.
 * <p>
 * By default, this class uses {@link scala.collection.immutable.SortedMap}s
 * with an implicit ordering passed to its constructor for storing its values.
 * However, this is not a strict requirement.
 * You can change this default property by overriding {@link #newDirectory},
 * e.g. in order to return a new {@link scala.collection.mutable.LinkedHashMap}
 * <p>
 * This class is <em>not</em> thread-safe!
 * 
 * @param  <V> the type of the values in this map.
 * @author Christian Schlichtherle
 */
class PathMap[K >: Null <: AnyRef, V] protected
(implicit val composition: Composition[K], ordering: Ordering[K])
extends mutable.Map[K, V] with mutable.MapLike[K, V, PathMap[K, V]] {

  private[this] var _root: Node[K, V] = _
  private var _size: Int = _

  reset()

  implicit private def self = this

  private def reset() { _root = new Root; _size = 0}

  override def size = _size

  override def clear() = reset()

  protected def newDirectory[V]: mutable.Map[K, V] =
    new mutable.ImmutableMapAdaptor(immutable.SortedMap.empty)

  override def empty = new PathMap[K, V]

  override def iterator = _root recursiveEntriesIterator (None)

  override def get(path: K) = node(path) flatMap (_ value)

  def node(path: K): Option[Node[K, V]] = node(Option(path))

  private def node(path: Option[K]): Option[Node[K, V]] = {
    path match {
      case Some(path) =>
        path match {
          case composition(parent, segment) =>
            node(parent) flatMap (_.get(segment))
        }
      case None =>
        Some(_root)
    }
  }

  def list(path: K): Option[Iterable[(K, V)]] = list(Option(path))

  private def list(path: Option[K]) = node(path) map (_ list path)

  override def +=(entry: (K, V)) = {
    add(entry._1, entry._2)
    this
  }

  def add(path: K, value: V): Node[K, V] = add(Option(path), Some(value))

  private def add(path: Option[K], value: Option[V]): Node[K, V] = {
    path match {
      case Some(path) =>
        path match {
          case composition(parent, segment) =>
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
          case composition(parent, segment) =>
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

  def apply[K >: Null <: AnyRef : Ordering, V](composition: Composition[K]) =
    new PathMap[K, V]()(composition, implicitly[Ordering[K]])

  def apply[V](separator: Char): PathMap[String, V] =
    apply(new StringComposition(separator))

  class Node[K >: Null <: AnyRef, V] private[PathMap] (
    private[this] val parent: Option[(Node[K, V], K)],
    private[this] var _value: Option[V])
  (implicit map: PathMap[K, V]) {

    private[this] val _members = map.newDirectory[Node[K, V]]

    if (_value isDefined) map._size += 1

    def address: (PathMap[K, V], Option[K]) = {
      val (node, segment) = parent.get
      val (map, path) = node.address
      map -> Some(map.composition(path, segment))
    }

    def path = address._2

    def value = _value

    private[PathMap] def value_=(value: Option[V])
    (implicit map: PathMap[K, V]) {
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

    private[PathMap] def isEmpty = _value.isEmpty && 0 == _members.size

    private[PathMap] final def get(segment: K) = _members get segment

    private[PathMap] final def add(segment: K, value: Option[V])
    (implicit map: PathMap[K, V]) = {
      _members get segment match {
        case Some(node) =>
          if (value isDefined) node value = value
          node
        case None =>
          val node = new Node[K, V](Some(this, segment), value)
          _members += segment -> node
          node
      }
    }

    private[PathMap] final def remove(segment: K)(implicit map: PathMap[K, V]) {
      _members get segment map { node =>
        node value = None
        if (node isEmpty) _members -= segment
      }
    }

    private[PathMap] final def recursiveEntriesIterator(path: Option[K])
    (implicit composition: Composition[K]): Iterator[(K, V)] = {
      entry(path).iterator ++ _members.iterator.flatMap {
        case (segment, node) =>          
          node recursiveEntriesIterator Some(composition(path, segment))
      }
    }

    private def entry(path: Option[K]) = _value map (path.orNull -> _)

    private[PathMap] def list(path: Option[K])
    (implicit composition: Composition[K]) = {
      _members.toIterable flatMap {
        case (segment, node) => node.value map (composition(path, segment) -> _)
      }
    }

    def members = {
      _members.toIterable withFilter {
        case (segment, node) => node.value isDefined
      }
    }
  } // Node

  private final class Root[K >: Null <: AnyRef, V](implicit map: PathMap[K, V])
  extends Node[K, V](None, None) {
    override def address = map -> None
  } // Root

  trait Composition[K] extends ((Option[K], K) => K) {
    /** The composition method for injection. */
    override def apply(parent: Option[K], segment: K): K

    /**
     * The decomposition method for extraction.
     * Note that the second element of the returned tuple should not share any
     * memory with the given path - otherwise you might not achieve any heap
     * space savings!
     */
    def unapply(path: K): Some[(Option[K], K)]
  } // Composition

  final case class StringComposition(separator: Char)
  extends Composition[String] {
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
  } // StringComposition
} // PathMap
