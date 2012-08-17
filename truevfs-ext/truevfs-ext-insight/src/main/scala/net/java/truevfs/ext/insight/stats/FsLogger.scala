/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.insight.stats

import java.util.concurrent.atomic._
import javax.annotation.concurrent._
import FsLogger._

/**
 * A lock-free logger for [[net.java.truevfs.ext.insight.stats.FsStatistics]]
 * All operations get logged at offset zero.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class FsLogger(val size: Int) {

  def this() = this(defaultSize)

  private[this] val _position = new AtomicInteger
  private[this] val _stats = {
    val s = new AtomicReferenceArray[FsStatistics](size)
    for (i <- 0 until size) s.set(i, FsStatistics())
    s
  }
  private[this] val readThreads = collection.mutable.Set[Long]()
  private[this] val writeThreads = collection.mutable.Set[Long]()
  private[this] val syncThreads = collection.mutable.Set[Long]()

  private def position = _position.get

  private def index(offset: Int) = {
    if (offset < 0 || size <= offset) throw new IllegalArgumentException
    var index = position - offset
    if (index < 0) index += size
    index
  }

  def format(offset: Int) = {
    val max = size - 1
    if (offset < 0 || max < offset) throw new IllegalArgumentException
    "%%0%dd".format(length(max)).format(offset)
  }

  def stats(offset: Int) = _stats.get(index(offset))
  def current = stats(0)

  /**
    * Logs a read operation with the given sample data and returns a new
    * object to reflect the updated statistics.
    * The sequence number of the returned object will be incremented and may
    * eventually overflow to zero.
    *
    * @param  nanos the execution time in nanoseconds.
    * @param  bytes the number of bytes read.
    * @return A new object which reflects the updated statistics.
    * @throws IllegalArgumentException if any parameter value is negative.
    */
  def logRead(nanos: Long, bytes: Int): IoStatistics = {
    val threads = log(readThreads)
    while (true) {
      val expected = current
      val updated = expected logRead (nanos, bytes, threads)
      if (_stats weakCompareAndSet (position, expected, updated))
        return updated.readStats
    }
    throw new AssertionError
  }

  /**
   * Logs a write operation with the given sample data and returns a new
   * object to reflect the updated statistics.
   * The sequence number of the returned object will be incremented and may
   * eventually overflow to zero.
   *
   * @param  nanos the execution time in nanoseconds.
   * @param  bytes the number of bytes written.
   * @return A new object which reflects the updated statistics.
   * @throws IllegalArgumentException if any parameter is negative.
   */
  def logWrite(nanos: Long, bytes: Int): IoStatistics = {
    val threads = log(writeThreads)
    while (true) {
      val expected = current
      val updated = expected logWrite (nanos, bytes, threads)
      if (_stats weakCompareAndSet (position, expected, updated))
        return updated.writeStats
    }
    throw new AssertionError
  }

  /**
   * Logs a sync operation with the given sample data and returns a new
   * object to reflect the updated statistics.
   * The sequence number of the returned object will be incremented and may
   * eventually overflow to zero.
   *
   * @param  nanos the execution time in nanoseconds.
   * @return A new object which reflects the updated statistics.
   * @throws IllegalArgumentException if any parameter value is negative.
   */
  def logSync(nanos: Long): SyncStatistics = {
    val threads = log(syncThreads)
    while (true) {
      val expected = current
      val updated = expected logSync (nanos, threads)
      if (_stats weakCompareAndSet (position, expected, updated))
        return updated.syncStats
    }
    throw new AssertionError
  }

  def rotate() = {
    val n = next()
    _stats.set(n, FsStatistics())
    readThreads clear ()
    writeThreads clear ()
    syncThreads clear ()
    n
  }

  private def next(): Int = {
    while (true) {
      val expected = _position.get
      var updated = expected + 1
      if (size <= updated) updated -= size
      if (_position.compareAndSet(expected, updated)) return updated
    }
    throw new AssertionError
  }
}

@ThreadSafe
object FsLogger {

  private[this] val defaultSizePropertyKey = classOf[FsLogger].getName + ".defaultSize"
  private val defaultSize = Integer.getInteger(defaultSizePropertyKey, 10)
  private[this] val maxValues = Array(
    9, 99, 999, 9999, 99999, 999999, 9999999, 99999999, 999999999,
    Integer.MAX_VALUE
  )

  private def length(x: Int) = {
    assert(0 <= x)
    var i = 0
    while (x > maxValues{ val j = i; i += 1; j }) {
    }
    i
  }

  private def hash(thread: Thread) = {
    var hash = 17L;
    hash = 31 * hash + System.identityHashCode(thread)
    hash = 31 * hash + thread.getId
    hash
  }

  /**
   * Adds a fingerprint of the current thread to the given mutable set and returns its size.
   *
   * @param  set the mutable set to use for logging.
   * @return the resulting size of the set
   */
  private def log(set: collection.mutable.Set[Long]) = {
    set += hash(Thread.currentThread)
    set.size
  }
}
