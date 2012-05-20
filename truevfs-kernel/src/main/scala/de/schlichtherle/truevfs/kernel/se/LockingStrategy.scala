/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truevfs.kernel.se

import de.schlichtherle.truevfs.kernel._
import java.util.Random
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.locks.Lock
import java.util.concurrent.TimeUnit

/**
 * Implements a locking strategy with enumerable options to control dead lock
 * prevention.
 * Note that in order to make this class work as designed, you MUST call
 * {@link #apply} for <em>each and every</em> lock which may participate in a
 * dead lock - even if you only want to call {@link Lock#lock()}!
 * Otherwise, the lock accounting in this class will not work!
 * <p>
 * This class does not use timed waiting, so there's no point in feeding it
 * with fair locks.
 * 
 * @see    NeedsLockRetryException
 * @author Christian Schlichtherle
 */
private abstract class LockingStrategy {

  def acquire(lock: Lock): Unit

  /**
   * Holds the given lock while calling the given operation.
   * <p>
   * If this is the first execution of this method on the call stack of the
   * current thread, then the lock gets acquired using {@link Lock#lock()}.
   * Once the lock has been acquired the operation gets called.
   * If the operation fails with a {@link NeedsLockRetryException}, then
   * the lock gets temporarily released and the current thread gets paused
   * for a small random amount of milliseconds before this algorithm starts
   * over again.
   * <p>
   * If this is <em>not</em> the first execution of this method on the call
   * stack of the current thread however, then the lock gets acquired
   * according to the strategy defined by this object.
   * If acquiring the lock fails, then a {@code NeedsLockRetryException} gets
   * thrown.
   * Once the lock has been acquired the operation gets called just as if
   * this was the first execution of this method on the call stack of the
   * current thread.
   * <p>
   * If this method is called recursively on the {@link #FAST_LOCK} strategy,
   * then dead locks get effectively prevented by temporarily unwinding the
   * stack and releasing all locks for a small random amount of milliseconds.
   * However, this requires some cooperation by the caller AND the given
   * operation: Both MUST terminate their execution in a consistent state,
   * even if a {@link NeedsLockRetryException} occurs!
   * 
   * @param  <V> the return type of the operation.
   * @param  <X> the exception type of the operation.
   * @param  lock The lock to hold while calling the operation.
   * @param  operation The operation to protect by the lock.
   * @return The result of the operation.
   * @throws X As thrown by the operation.
   * @throws NeedsLockRetryException See above.
   */
  def apply[V](lock: Lock, operation: => V): V = {
    import LockingStrategy._

    val account = accounts.get
    if (0 < account.lockCount) {
      acquire(lock)
      account.lockCount += 1
      try {
        return operation
      } finally {
        account.lockCount -= 1
        lock.unlock()
      }
    } else {
      try {
        while (true) {
          try {
            lock.lock()
            account.lockCount += 1
            try {
              return operation
            } finally {
              account.lockCount -= 1
              lock.unlock()
            }
          } catch {
            case _: NeedsLockRetryException =>
              account.arbitrate
          }
        }
        throw new AssertionError("dead code")
      } finally {
        accounts.remove()
      }
    }
  }
}

private object LockingStrategy {
  private val ARBITRATE_MAX_MILLIS = 100;
  val ACQUIRE_TIMEOUT_MILLIS = ARBITRATE_MAX_MILLIS
  private val accounts = new ThreadLocalAccount

  private final class ThreadLocalAccount extends ThreadLocal[Account] {
    override def initialValue = Account(ThreadLocalRandom.current)
  }

  private case class Account(rnd: Random) {
    var lockCount = 0

    def arbitrate {
      try {
        Thread.sleep(1 + rnd.nextInt(ARBITRATE_MAX_MILLIS))
      } catch {
        case _: InterruptedException =>
          Thread.currentThread.interrupt() // restore
      }
    }
  }

  def lockCount = accounts.get.lockCount

  /**
   * Acquires the given lock using {@link Lock#tryLock()}.
   */
  object FAST_LOCK extends LockingStrategy {
    override def acquire(lock: Lock) {
      if (!lock.tryLock)
        throw NeedsLockRetryException.get
    }
  }

  /**
   * Acquires the given lock using {@link Lock#tryLock(long, TimeUnit)}.
   */
  object TIMED_LOCK extends LockingStrategy {
    override def acquire(lock: Lock) {
      try {
        if (!lock.tryLock(ACQUIRE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS))
          throw NeedsLockRetryException.get
      } catch {
        case _: InterruptedException =>
          Thread.currentThread.interrupt() // restore
          throw NeedsLockRetryException.get
      }
    }
  }

  /**
   * Acquires the given lock using {@link Lock#lock()}.
   */
  object DEAD_LOCK extends LockingStrategy {
    override def acquire(lock: Lock) {
      lock.lock()
    }
  }
}
