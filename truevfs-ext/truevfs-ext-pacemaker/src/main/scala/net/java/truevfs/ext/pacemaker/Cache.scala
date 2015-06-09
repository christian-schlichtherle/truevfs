package net.java.truevfs.ext.pacemaker

import java.util.concurrent.locks.{Lock, ReadWriteLock, ReentrantReadWriteLock}
import java.util.{concurrent => juc}
import java.{util => ju}

import net.java.truecommons.shed.{Filter, HashMaps}

/** A simple, generic cache.
  * Note that unlike other caches, whenever a map entry gets evicted, it gets
  * added to a concurrent map which can get queried using the [[evictedKeySet]]
  * and [[evictedValues]] methods for further cleanup.
  * This class is thread-safe.
  *
  * @tparam K the type of the keys
  * @tparam V the type of the values
  * @author Christian Schlichtherle
  */
private final class Cache[K, V](initialMaximumSize: Int) {

  private val lock: ReadWriteLock = new ReentrantReadWriteLock
  private val readLock = lock.readLock
  private val writeLock = lock.writeLock

  private val evicted: juc.ConcurrentMap[K, V] = new juc.ConcurrentHashMap
  private val cached = new CacheMap

  @volatile
  private var _maximumSize: Int = _

  maximumSize = initialMaximumSize

  def evictedKeySet = evicted.keySet
  def evictedValues = evicted.values

  def maximumSize = _maximumSize
  def maximumSize_=(maximumSize: Int) {
    require(0 <= maximumSize)
    _maximumSize = maximumSize
  }

  def get(key: AnyRef) =
    // The lookup needs to be write-locked because the access-ordered cache map
    // may get modified to record the access to the key!
    writeLocked { cached get key }

  def put(key: K, value: V) = writeLocked { cached put (key, value) }

  def remove(key: AnyRef) = writeLocked { cached remove key }

  def existsValue(filter: Filter[_ >: V]) =
    readLocked { cached existsValue filter }

  private def readLocked[A] = locked[A](readLock) _

  private def writeLocked[A] = locked[A](writeLock) _

  private def locked[A](lock: Lock)(block: => A) = {
    lock lock ()
    try {
      block
    } finally {
      lock unlock ()
    }
  }

  private final class CacheMap
    extends ju.LinkedHashMap[K, V](HashMaps initialCapacity initialMaximumSize, 0.75f, true) {

    override def removeEldestEntry(entry: ju.Map.Entry[K, V]) =
      if (size > maximumSize) {
        evicted put (entry.getKey, entry.getValue)
        true
      } else {
        false
      }

    override def put(key: K, value: V) = {
      evicted remove key
      super.put(key, value)
    }

    override def remove(key: Object) = {
      evicted remove key
      super.remove(key)
    }

    def existsValue(filter: Filter[_ >: V]): Boolean = {
      val iterator = values.iterator
      while (iterator.hasNext)
        if (filter accept iterator.next)
          return true
      false
    }
  }
}
