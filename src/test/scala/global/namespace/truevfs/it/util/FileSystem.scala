/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.it.util

import global.namespace.truevfs.it.util
import global.namespace.truevfs.it.util.FileSystem._

import java.util.Comparator
import java.{util => ju}
import scala.collection._
import scala.jdk.CollectionConverters._

/**
  * A (virtual) file system is a mutable map from decomposable keys, named
  * ''paths'', to arbitrary values, named ''entries''.
  * The standard use case for this class is for mapping path name strings which
  * can get decomposed into segments by splitting them with a separator
  * character such as `'/'`.
  * However, this class works with any generic decomposable key type for which
  * a [[util.FileSystem.Composition]] and a
  * [[util.FileSystem.DirectoryFactory]] exist.
  *
  * Using this class helps to save some heap space if the paths address deeply
  * nested directory trees where many path segments can get shared between
  * mapped entries &mdash; provided that no other references to the paths are
  * held!
  *
  * This class supports both `null` paths and entries.
  *
  * This class is not thread-safe.
  *
  * @tparam K the type of the paths (keys) in this (virtual) file system (map).
  * @tparam V the type of the entries (values) in this (virtual) file system
  *         (map).
  * @author Christian Schlichtherle
  */
final class FileSystem[K >: Null, V](
  implicit val composition: Composition[K],
  val directoryFactory: DirectoryFactory[K]
) extends mutable.Map[K, V] {

  private[this] var _root: INode[K, V] = _

  private var _size: Int = _

  reset()

  implicit private def self: FileSystem[K, V] = this

  private def reset(): Unit = { _root = new Root; _size = 0}

  override def size: Int = _size

  override def clear(): Unit = reset()

  override def empty: FileSystem[K, V] = new FileSystem[K, V]

  override def iterator: Iterator[(K, V)] = iterator(None, _root)

  private def iterator(path: Option[K], node: Node[K, V]): Iterator[(K, V)] = {
    node.entry.map(path.orNull -> _).iterator ++ node.iterator.flatMap {
      case (matchedSegment, matchedNode) => iterator(Some(composition(path, matchedSegment)), matchedNode)
    }
  }

  def list(path: K): Option[Iterator[(K, V)]] = list(Option(path))

  private def list(path: Option[K]): Option[Iterator[(K, V)]] = {
    node(path) map (_.iterator flatMap {
      case (segment, node) => node.entry map (composition(path, segment) -> _)
    })
  }

  override def get(path: K): Option[V] = node(path) flatMap (_.entry)

  def node(path: K): Option[Node[K, V]] = node(Option(path))

  private def node(optPath: Option[K]): Option[INode[K, V]] = {
    optPath match {
      case Some(path) =>
        path match {
          case composition(parent, segment) =>
            node(parent) flatMap (_ get segment)
        }
      case None =>
        Some(_root)
    }
  }

  override def addOne(kv: (K, V)): this.type = { link(kv._1, kv._2); this }

  def link(path: K, entry: V): Node[K, V] = link(Option(path), Some(entry))

  private def link(optPath: Option[K], optEntry: Option[V]): INode[K, V] = {
    optPath match {
      case Some(path) =>
        path match {
          case composition(parent, segment) =>
            link(parent, None) link (segment, optEntry)
        }
      case None =>
        if (optEntry.isDefined) _root.entry = optEntry
        _root
    }
  }

  override def subtractOne(path: K): this.type = { unlink(path); this }

  def unlink(path: K): Unit = unlink(Option(path))

  private def unlink(optPath: Option[K]): Unit = {
    optPath match {
      case Some(path) =>
        path match {
          case composition(parent, segment) =>
            node(parent) foreach { node =>
              node unlink segment
              if (node.isDead) unlink(parent)
            }
        }
      case None =>
        _root.entry = None
    }
  }

  override def className: String = "FileSystem"
}

/**
  * @author Christian Schlichtherle
  */
object FileSystem {

  def apply[K >: Null, V](
    composition: Composition[K],
    directoryFactory: DirectoryFactory[K]
  ): FileSystem[K, V] = new FileSystem[K, V]()(composition, directoryFactory)

  def apply[V](
    separator: Char,
    directoryFactory: DirectoryFactory[String] = new SortedDirectoryFactory
  ): FileSystem[String, V] = apply[String, V](new StringComposition(separator), directoryFactory)

  /** A file system node. */
  sealed abstract class Node[K >: Null, +V] extends Iterable[(K, Node[K, V])] {

    def path: Option[K]

    final def isRoot: Boolean = path.isEmpty

    def entry: Option[V]

    final def isGhost: Boolean = entry.isEmpty

    final def isLeaf: Boolean = isEmpty

    final override def className: String = "Node"

    final override def toString(): String = {
      className + "(path=" + path + ", isLeaf=" + isLeaf + ", entry=" + entry + ")"
    }
  }

  private class INode[K >: Null, V] protected (
    private[this] val parent: Option[(INode[K, V], K)],
    private[this] var _entry: Option[V]
  ) (implicit fs: FileSystem[K, V])
  extends Node[K, V] {

    private[this] val _members = fs.directoryFactory.create[INode[K, V]]

    if (_entry.isDefined) fs._size += 1

    override def iterator: Iterator[(K, INode[K, V])] = _members.iterator

    override def foreach[U](f: ((K, Node[K, V])) => U): Unit = _members foreach f

    override def size: Int = _members.size

    override def path: Option[K] = address._2

    def address: (FileSystem[K, V], Option[K]) = {
      val (node, segment) = parent.get
      val (fs, path) = node.address
      fs -> Some(fs composition (path, segment))
    }

    override def entry: Option[V] = _entry

    def entry_=(entry: Option[V])(implicit fs: FileSystem[K, V]): Unit = {
      // HC SVNT DRACONES!
      if (_entry.isDefined) {
        if (entry.isEmpty)
          fs._size -= 1
      } else {
        if (entry.isDefined)
          fs._size += 1
      }
      _entry = entry
    }

    def get(segment: K): Option[INode[K, V]] = _members get segment

    def link(segment: K, entry: Option[V])(implicit fs: FileSystem[K, V]): INode[K, V] = {
      _members get segment match {
        case Some(node) =>
          if (entry.isDefined) node.entry = entry
          node
        case None =>
          val node = new INode[K, V](Some(this, segment), entry)
          _members += segment -> node
          node
      }
    }

    def unlink(segment: K)(implicit fs: FileSystem[K, V]): Unit = {
      _members get segment foreach { node =>
        node.entry = None
        if (node.isLeaf) _members -= segment
      }
    }

    def isDead: Boolean = isGhost && isLeaf
  }

  private final class Root[K >: Null, V](implicit fs: FileSystem[K, V]) extends INode[K, V](None, None) {

    override def address: (FileSystem[K, V], None.type) = fs -> None
  }

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
  }

  final class StringComposition(separator: Char) extends Composition[String] {

    override def apply(optParent: Option[String], segment: String): String = {
      optParent match {
        case Some(parent) => parent + segment
        case None => segment
      }
    }

    override def unapply(path: String): Some[(Option[String], String)] = {
      val i = path.lastIndexOf(separator)
      if (0 <= i) {
        // Don't share sub-strings with the FileSystem!
        Some(Some(path substring (0, i)) -> new String(path substring i))
      } else {
        // There's no point in copying here!
        Some(None, path)
      }
    }
  }

  sealed trait DirectoryFactory[K] {

    def create[V]: mutable.Map[K, V]
  }

  final class HashedDirectoryFactory[K] extends DirectoryFactory[K] {

    def create[V]: mutable.Map[K, V] = new mutable.HashMap
  }

  final class SortedDirectoryFactory[K : Ordering] extends DirectoryFactory[K] {

    def create[V]: mutable.Map[K, V] = new ju.TreeMap[K, V](implicitly[Comparator[K]]).asScala
  }

  final class LinkedDirectoryFactory[K] extends DirectoryFactory[K] {

    override def create[V]: mutable.Map[K, V] = new mutable.LinkedHashMap
  }
}
