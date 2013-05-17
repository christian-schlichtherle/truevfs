/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl

import net.java.truecommons.shed._
import collection.JavaConverters._
import java.io._
import java.util.concurrent._
import java.util.concurrent.locks._
import javax.annotation._
import javax.annotation.concurrent._
import scala.util.control._

/** Accounts for [[java.io.Closeable]] resources.
  * 
  * For synchronization, each accountant uses a lock which has to be provided
  * to its constructor.
  * In order to start accounting for a closeable resource, call `start`.
  * In order to stop  accounting for a closeable resource, call `stop`.
  * 
  * Note that you ''must make sure'' not to use two instances of this class
  * which share the same lock!
  * Otherwise `waitOtherThreads` will not work as designed!
  * 
  * @param  lock the lock to use for accounting resources.
  * @see    FsResourceController
  * @author Christian Schlichtherle
  */
@ThreadSafe
private final class ResourceAccountant(override val lock: Lock)
extends LockAspect[Lock] {
  import ResourceAccountant._

  private[this] val condition = lock.newCondition

  /** Starts accounting for the given closeable resource.
    * 
    * @param resource the closeable resource to start accounting for.
    */
  def startAccountingFor(@WillCloseWhenClosed resource: Closeable) {
    accounts += resource -> Account(this)
  }

  /** Stops accounting for the given closeable resource.
    * This method should be called from the implementation of the `close`
    * method of the given [[java.io.Closeable]].
    * 
    * @param resource the closeable resource to stop accounting for.
    */
  def stopAccountingFor(@WillNotClose resource: Closeable) {
    accounts.remove(resource) foreach { _ => locked (condition signalAll ()) }
  }

  /** Waits until all closeable resources which have been started accounting
    * for by ''other'' threads get stopped accounting for or a timeout occurs
    * or the current thread gets interrupted, whatever happens first.
    * 
    * Waiting for such resources can get cancelled immediately by interrupting
    * the current thread.
    * Unless the number of closeable resources which have been accounted for
    * by ''all'' threads is zero, this will leave the interrupt status of the
    * current thread cleared.
    * If no such foreign resources exist, then interrupting the current thread
    * does not have any effect.
    * 
    * Upon return of this method, threads may immediately start accounting
    * for closeable resources again unless the caller has acquired the lock
    * provided to the constructor - use with care!
    * 
    * Note that this method '''will not work''' if any two instances of this
    * class share the same lock provided to their constructor!
    *
    * @param timeout the number of milliseconds to await the closing of
    *        resources which have been accounted for by ''other''
    *        threads once the lock has been acquired.
    *        If this is non-positive, then there is no timeout for waiting.
    */
  def awaitClosingOfOtherThreadsResources(timeout: Long) {
    locked {
      try {
        if (0 < timeout) {
          var toWait = TimeUnit.MILLISECONDS toNanos timeout;
          while (0 < toWait && resources.needsWaiting)
            toWait = condition awaitNanos toWait
        } else {
          while (resources.needsWaiting)
            condition await ()
        }
      } catch {
        case _: InterruptedException =>
          // Fix rare racing condition between Thread.interrupt() and
          // Condition.signalAll() events.
          if (0 == resources.total)
            Thread.currentThread.interrupt()
      }
    }
  }

  /** Returns the number of closeable resources which have been accounted for.
    * The first element contains the number of closeable resources which have
    * been created by the current thread (''local'').
    * The second element contains the number of closeable resources which have
    * been created by all threads (''total'').
    * Mind that this value may reduce concurrently, even while the lock is
    * held, so it should ''not'' get cached!
    * 
    * @return The number of closeable resources which have been accounted for.
    */
  def resources = {
    val currentThread = Thread.currentThread
    var local, total = 0
    for (account <- accounts.values if account.accountant eq this) {
      if (account.owner eq currentThread) local += 1
      total += 1
    }
    Resources(local, total)
  }

  /** For each accounted closeable resource, stops accounting for it and closes
    * it.
    * Upon return of this method, threads may immediately start accounting for
    * closeable resources again unless the caller also locks the lock provided
    * to the constructor - use with care!
    */
  def closeAllResources[X <: Exception](handler: ExceptionHandler[_ >: IOException, X]) {
    assert(null != handler)
    lock lock ()
    try {
      for ((closeable, account) <- accounts if account.accountant eq this) {
        accounts -= closeable
        try {
          // This should trigger an attempt to remove the closeable from the
          // map, but it can cause no ConcurrentModificationException because
          // the entry is already removed and a ConcurrentHashMap doesn't do
          // that anyway.
          closeable close ()
        } catch {
          case ex: IOException =>
            handler warn ex // may throw an exception!
        }
      }
    } finally {
      try {
        condition signalAll ()
      } finally {
        lock unlock ()
      }
    }
  }
} // class ResourceAccountant

private object ResourceAccountant {

  /** The map of all accounted closeable resources.
    * The initial capacity for the hash map accounts for the number of
    * available processors, a 90% blocking factor for typical I/O and a 2/3
    * map resize threshold.
    */
  private val accounts = {
    val threads = Runtime.getRuntime.availableProcessors() * 10
    val initialCapacity = HashMaps.initialCapacity(threads)
    new ConcurrentHashMap[Closeable, Account](initialCapacity, 0.75f, threads).asScala
  }

  private final case class Account(accountant: ResourceAccountant) {
    val owner = Thread.currentThread
  }

  final case class Resources(local: Int, total: Int) {
    def needsWaiting = local < total
  }
} // object ResourceAccountant
