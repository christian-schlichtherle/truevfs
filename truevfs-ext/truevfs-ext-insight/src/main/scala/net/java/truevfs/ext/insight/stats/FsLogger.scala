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

  private[this] val _stats = {
    val s = new AtomicReferenceArray[FsStatistics](size)
    for (i <- 0 until size) s.set(i, FsStatistics())
    s
  }
  private[this] val _position = new AtomicReference[Int]
  private[this] val _readThreads = new AtomicReference(Set.empty: Set[Long])
  private[this] val _writeThreads = new AtomicReference(Set.empty: Set[Long])
  private[this] val _syncThreads = new AtomicReference(Set.empty: Set[Long])

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

  private def update(next: FsStatistics => FsStatistics): FsStatistics = {
    while (true) {
      val expect = current
      val update = next(expect)
      if (_stats weakCompareAndSet (position, expect, update)) return update
    }
    throw new AssertionError
  }

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
  def logRead(nanos: Long, bytes: Int) = {
    val threads = logCurrentThread(_readThreads)
    update(_ logRead (nanos, bytes, threads)).readStats
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
  def logWrite(nanos: Long, bytes: Int) = {
    val threads = logCurrentThread(_writeThreads)
    update(_ logWrite (nanos, bytes, threads)).writeStats
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
  def logSync(nanos: Long) = {
    val threads = logCurrentThread(_syncThreads)
    update(_ logSync (nanos, threads)).syncStats
  }

  def rotate() = {
    val empty: Set[Long] = Set.empty
    val n = next()
    _stats.set(n, FsStatistics())
    _readThreads.set(empty)
    _writeThreads.set(empty)
    _syncThreads.set(empty)
    n
  }

  private def next() = {
    atomic(_position) { expect =>
      var update = expect + 1
      if (size <= update) update -= size
      update
    }
  }
}

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
    var hash = 17L
    hash = 31 * hash + System.identityHashCode(thread)
    hash = 31 * hash + thread.getId
    hash
  }

  private def atomic[V](ref: AtomicReference[V])(next: V => V): V = {
    while (true) {
      val expect = ref.get
      val update = next(expect)
      if (ref.weakCompareAndSet(expect, update)) return update
    }
    throw new AssertionError
  }

  /**
   * Adds a fingerprint of the current thread to the referenced set and returns its size.
   *
   * @param  ref the atomic reference to the immutable set to use for logging.
   * @return the resulting size of the set
   */
  private def logCurrentThread(ref: AtomicReference[Set[Long]]) = {
    val fp = hash(Thread.currentThread)
    atomic(ref)(_ + fp).size
  }
}
