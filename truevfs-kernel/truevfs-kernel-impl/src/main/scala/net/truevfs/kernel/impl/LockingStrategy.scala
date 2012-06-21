/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.impl

import net.truevfs.kernel.spec._
import java.util._
import java.util.concurrent._
import java.util.concurrent.locks._
import javax.annotation.concurrent._

/** Implements a locking strategy with enumerable options to control dead lock
  * prevention.
  * Note that in order to make this class work as designed, you '''must''' call
  * `apply` for ''each and every'' lock which may participate in a
  * dead lock!
  * Otherwise, the locking strategy will not work!
  * 
  * @see    NeedsLockRetryException
  * @author Christian Schlichtherle
  */
@ThreadSafe
private abstract class LockingStrategy {
  import LockingStrategy._

  protected def acquire(lock: Lock): Unit

  /** Holds the given lock while calling the given operation.
    * 
    * If this is the first execution of this method on the call stack of the
    * current thread, then the lock gets acquired using `Lock.lock()`.
    * Once the lock has been acquired the operation gets called.
    * If the operation fails with a
    * [[net.truevfs.kernel.impl.NeedsLockRetryException]], then
    * the lock gets temporarily released and the current thread gets paused
    * for a small random amount of milliseconds before this algorithm starts
    * over again.
    * 
    * If this is ''not'' the first execution of this method on the call
    * stack of the current thread however, then the lock gets acquired
    * according to the strategy defined by this object.
    * If acquiring the lock fails, then a
    * [[net.truevfs.kernel.impl.NeedsLockRetryException]] gets
    * thrown.
    * Once the lock has been acquired the operation gets called just as if
    * this was the first execution of this method on the call stack of the
    * current thread.
    * 
    * If this method is called recursively on the `fastLock` or `timedLock`
    * strategy, then dead locks get effectively prevented by temporarily
    * unwinding the stack and releasing all locks for a small random amount of
    * milliseconds.
    * However, this requires some cooperation by the caller '''and''' the given
    * operation: Both '''must''' terminate their execution in a consistent
    * state, because a
    * [[net.truevfs.kernel.impl.NeedsLockRetryException]] may occur
    * anytime!
    * 
    * @tparam V the return type of the operation.
    * @param  lock The lock to hold while calling the operation.
    * @param  operation The operation to protect by the lock.
    * @return The result of the operation.
    * @throws NeedsLockRetryException See above.
    */
  def apply[V](lock: Lock)(operation: => V): V = {
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
              lock unlock()
            }
          } catch {
            case _: NeedsLockRetryException => account.arbitrate()
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
  private val arbitrateMaxMillis = 100;
  val acquireTimeoutMillis = arbitrateMaxMillis
  private val accounts = new ThreadLocalAccount

  private final class ThreadLocalAccount extends ThreadLocal[Account] {
    override def initialValue = Account(ThreadLocalRandom.current)
  }

  private case class Account(rnd: Random) {
    var lockCount = 0

    def arbitrate() {
      try {
        Thread sleep (1 + rnd.nextInt(arbitrateMaxMillis))
      } catch {
        case _: InterruptedException =>
          Thread.currentThread.interrupt() // restore
      }
    }
  }

  def lockCount = accounts.get lockCount

  /** Acquires the given lock using `Lock.tryLock()`. */
  object fastLocked extends LockingStrategy {
    override protected def acquire(lock: Lock) {
      if (!lock.tryLock())
        throw NeedsLockRetryException()
    }
  }

  /** Acquires the given lock using `Lock.tryLock(long, TimeUnit)`. */
  object timedLocked extends LockingStrategy {
    override protected def acquire(lock: Lock) {
      try {
        if (!(lock tryLock (acquireTimeoutMillis, TimeUnit.MILLISECONDS)))
          throw NeedsLockRetryException()
      } catch {
        case ex: InterruptedException =>
          Thread.currentThread interrupt() // restore
          throw NeedsLockRetryException()
      }
    }
  }

  /** Acquires the given lock using `Lock.lock()`. */
  object deadLocked extends LockingStrategy {
    override protected def acquire(lock: Lock) {
      lock.lock()
    }
  }
}
