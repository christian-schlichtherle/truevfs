package net.java.truevfs.ext.pacemaker

import java.util.Map.Entry
import java.util.concurrent.locks.{Lock, ReadWriteLock, ReentrantReadWriteLock}
import java.util.{concurrent => juc}
import java.{util => ju}
import scala.collection.JavaConverters._
import net.java.truecommons.shed.HashMaps

/** A simple cache set with a least-recently-used (LRU) eviction strategy.
  * Note that unlike other caches, whenever an item gets evicted, it gets
  * added to a concurrent set which can get queried using the [[evicted]] method
  * for further processing, e.g. close resources.
  * This class is thread-safe.
  *
  * @tparam I the type of the items
  * @author Christian Schlichtherle
  */
private final class LruCache[I <: AnyRef](initialMaximumSize: Int) {

  private val _lock: ReadWriteLock = new ReentrantReadWriteLock
  private val _readLock = _lock.readLock
  private val _writeLock = _lock.writeLock

  private val _evicted: juc.ConcurrentMap[I, Boolean] = new juc.ConcurrentHashMap
  private val _cached = new CacheMap

  @volatile
  private var _maximumSize: Int = _

  maximumSize = initialMaximumSize

  def evicted = _evicted.keySet

  def maximumSize = _maximumSize
  def maximumSize_=(maximumSize: Int) {
    require(0 <= maximumSize)
    _maximumSize = maximumSize
  }

  def add(item: I) = writeLocked { _cached put (item, true) }

  /** Records access to the given mount point.
    * This method has no effect if the given mount point is not present in this
    * LRU cache.
    */
  def recordAccess(item: I) {
    // The lookup needs to be write-locked because the access-ordered cache map
    // may get structurally modified as a side effect.
    writeLocked { _cached get item }
  }

  def remove(item: I) = writeLocked { _cached remove item }

  def exists(predicate: I => Boolean) =
    readLocked { _cached exists predicate }

  private def readLocked[V] = locked[V](_readLock) _

  private def writeLocked[V] = locked[V](_writeLock) _

  private def locked[V](lock: Lock)(block: => V) = {
    lock lock ()
    try {
      block
    } finally {
      lock unlock ()
    }
  }

  private final class CacheMap
    extends ju.LinkedHashMap[I, Boolean](HashMaps initialCapacity initialMaximumSize, 0.75f, true) {

    override def removeEldestEntry(entry: Entry[I, Boolean]) =
      if (size > maximumSize) {
        _evicted put (entry.getKey, entry.getValue)
        true
      } else {
        false
      }

    override def put(key: I, value: Boolean) = {
      _evicted remove key
      super.put(key, value)
    }

    override def remove(key: Object) = {
      _evicted remove key
      super.remove(key)
    }

    def exists(predicate: I => Boolean) = keySet.asScala exists predicate
  }
}
