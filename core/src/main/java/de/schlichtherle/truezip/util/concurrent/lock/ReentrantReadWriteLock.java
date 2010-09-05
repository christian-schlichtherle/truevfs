/*
 * Copyright (C) 2006-2010 Schlichtherle IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.schlichtherle.truezip.util.concurrent.lock;

import de.schlichtherle.truezip.util.Operation;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Similar to {@code java.util.concurrent.locks.ReentrantReadWriteLock}
 * with the following differences:
 * <ul>
 * <li>This class performs better than its overengineered colleague in JSE 5.
 * <li>This class provides locks which provide a different set of methods
 *     (with the same functionality in the common subset) in order to suit
 *     the particular needs of TrueZIP (see {@link ReentrantLock}).
 * </ul>
 * <p>
 * <b>Note:</b> In accordance with JSE 5, upgrading a read lock to a write
 * lock is not possible. Any attempt to do so will lock the current thread.
 * This is a constraint which can't be fixed properly: If this constraint
 * would not exist, two reader threads could try to upgrade from a read lock
 * to a write lock concurrently, effectively dead locking them.
 * By locking this thread immediately on any attempt to do so, this is
 * considered to be a programming error which can be easily fixed without
 * affecting any other thread.
 * <p>
 * To the contrary, it is possible to downgrade from a write lock to a
 * read lock. Please consult the JSE 5 Javadoc of the class
 * {@code java.util.concurrent.locks.ReentrantReadWriteLock}
 * for more information.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public final class ReentrantReadWriteLock implements ReadWriteLock {

    private static final String CLASS_NAME
            = "de.schlichtherle.truezip.util.concurrent.locks.ReentrantReadWriteLock";
    private static final Logger logger
            = Logger.getLogger(CLASS_NAME, CLASS_NAME);

    private final ReadLock readLock = new ReadLock();
    private final WriteLock writeLock = new WriteLock();

    private int writeLockCount; // exclusive
    private int  readLockCount; // shared

    //
    // Methods.
    //

    /**
     * Returns the lock for reading.
     * Like its cousin in JSE 1.5, the returned lock does <em>not</em>
     * support upgrading to a write lock.
     */
    public ReentrantLock readLock() {
        return readLock;
    }

    /**
     * Returns the lock for writing.
     * Like its cousin in JSE 5, the returned lock <em>does</em>
     * support downgrading to a read lock.
     */
    public ReentrantLock writeLock() {
        return writeLock;
    }

    /**
     * Runs the given action while the write lock is temporarily acquired
     * even if the read lock is already acquired by the current thread.
     * <p>
     * <b>Warning:</b> This method temporarily releases the read lock
     * before the write lock is temporarily acquired and the action is run!
     * Hence, the action must recheck the preconditions for running it
     * before it proceeds with the operations which require the write lock.
     * <p>
     * Upon return, the hold count of the read and write lock for the current
     * thread is fully restored, even if the action throws a throwable.
     *
     * @param action The action to run while the write lock is acquired.
     * @throws NullPointerException If {@code action} is {@code null}.
     * @throws Throwable Upon the discretion of {@code action}.
     */
    public <T extends Throwable> void runWriteLocked(final Operation<T> action)
    throws T {
        if (action == null)
            throw new NullPointerException();

        if (writeLock.isLockedByCurrentThread()) {
            // Calls to *.unlock/lock() are redundant.
            action.run();
        } else {
            // A read lock cannot get upgraded to a write lock.
            // Hence the following mess is required.
            // Note that this is not just a limitation of the current
            // implementation in JSE 5: If automatic upgrading were implemented,
            // two threads holding a read lock try to upgrade concurrently,
            // they would dead lock each other!
            final int readHoldCount = readLock.getLockCount();
            for (int c = readHoldCount; c > 0; c--)
                readLock.unlock();

            // Current thread may get deactivated exactly here!

            writeLock.lock();
            try {
                for (int c = readHoldCount; c > 0; c--)
                    readLock.lock();
                action.run(); // beware of side effects on locks!
            } finally {
                writeLock.unlock();
            }
        }
    }

    //
    // Private implementation:
    // The code repetetition in these methods isn't elegant, but it's
    // faster than the use of the strategy pattern and performance is
    // critical in this class.
    //

    private void lockRead() {
        final int writeHoldCount = writeLock.getLockCount();
        synchronized (this) {
            // Wait until no other writer has acquired a lock.
            while (writeLockCount > writeHoldCount) {
                assert writeHoldCount == 0 : "write lock isn't held exclusively!";
                try {
                    wait();
                } catch (InterruptedException ex) {
                    logger.log(Level.FINE, "interrupted", ex);
                    logger.log(Level.FINE, "continuing");
                }
            }
            assert writeLockCount == writeHoldCount : "write lock/hold mismatch!";
            readLockCount++;
        }
    }

    private void lockInterruptiblyRead()
    throws InterruptedException {
        final int writeHoldCount = writeLock.getLockCount();
        synchronized (this) {
            // Wait until no other writer has acquired a lock.
            while (writeLockCount > writeHoldCount) {
                assert writeHoldCount == 0 : "write lock isn't held exclusively!";
                wait();
            }
            assert writeLockCount == writeHoldCount : "write lock/hold mismatch!";
            readLockCount++;
        }
    }

    private boolean tryLockRead() {
        final int writeHoldCount = writeLock.getLockCount();
        synchronized (this) {
            // Check if another writer has acquired a lock.
            if (writeLockCount > writeHoldCount) {
                assert writeHoldCount == 0 : "write lock isn't held exclusively!";
                return false;
            }
            assert writeLockCount == writeHoldCount : "write lock/hold mismatch!";
            readLockCount++;
        }
        return true;
    }

    private synchronized void unlockRead() {
        readLockCount--;
        notifyAll();
    }

    private void lockWrite() {
        final int writeHoldCount = writeLock.getLockCount();
        synchronized (this) {
            if (writeHoldCount <= 0) { // If I'm not the writer...
                final int readHoldCount = readLock.getLockCount();
                // ... wait until no other writer and no readers have acquired a lock.
                while (readLockCount > 0 /*- readHoldCount*/ // mimic JSE 5: dead lock on lock upgrade!
                        || writeLockCount > writeHoldCount) {
                    try {
                        wait();
                    } catch (InterruptedException ex) {
                        logger.log(Level.FINE, "interrupted", ex);
                        logger.log(Level.FINE, "continuing");
                    }
                }
                assert readLockCount == readHoldCount : "read lock/hold mismatch!";
            }
            writeLockCount++;
        }
    }

    private void lockInterruptiblyWrite()
    throws InterruptedException {
        final int writeHoldCount = writeLock.getLockCount();
        synchronized (this) {
            if (writeHoldCount <= 0) { // If I'm not the writer...
                final int readHoldCount = readLock.getLockCount();
                // ... wait until no other writer and no readers have acquired a lock.
                while (readLockCount > 0 /* readHoldCount*/ // mimic JSE 5: dead lock on lock upgrade!
                        || writeLockCount > writeHoldCount) {
                    wait();
                }
                assert readLockCount == readHoldCount : "read lock/hold mismatch!";
            }
            writeLockCount++;
        }
    }

    private boolean tryLockWrite() {
        final int writeHoldCount = writeLock.getLockCount();
        synchronized (this) {
            if (writeHoldCount <= 0) { // If I'm not the writer...
                final int readHoldCount = readLock.getLockCount();
                // ... check if another reader or writer has acquired a lock.
                if (readLockCount > 0 /* readHoldCount*/ // mimic JSE 5: dead lock on lock upgrade!
                        || writeLockCount > writeHoldCount) {
                    return false;
                }
                assert readLockCount == readHoldCount : "read lock/hold mismatch!";
            }
            writeLockCount++;
        }
        return true;
    }

    private synchronized void unlockWrite() {
        writeLockCount--;
        notifyAll();
    }

    //
    // Inner classes:
    //

    private static abstract class AbstractLock
            extends ThreadLocal<Integer>
            implements ReentrantLock {

        @Override
        protected Integer initialValue() {
            return 0;
        }

        public final boolean isLockedByCurrentThread() {
            return get() > 0;
        }

        public final int getLockCount() {
            return get();
        }

        public void lock() {
            set(get() + 1);
        }

        public void unlock() {
            int lockCount = get();
            if (lockCount <= 0)
                throw new IllegalMonitorStateException();
            set(lockCount - 1);
        }
    }

    private class ReadLock extends AbstractLock {
        @Override
        public void lock() {
            lockRead();
            super.lock(); // increment thread local counter last
        }

        public void lockInterruptibly() throws InterruptedException {
            lockInterruptiblyRead();
            super.lock(); // increment thread local counter last
        }

        public boolean tryLock() {
            boolean locked = tryLockRead();
            if (locked)
                super.lock();
            return locked;
        }

        @Override
        public void unlock() {
            super.unlock(); // decrement thread local counter first
            unlockRead();
        }
    }

    private class WriteLock extends AbstractLock {
        @Override
        public void lock() {
            lockWrite();
            super.lock(); // increment thread local counter last
        }

        public void lockInterruptibly() throws InterruptedException {
            lockInterruptiblyWrite();
            super.lock(); // increment thread local counter last
        }

        public boolean tryLock() {
            boolean locked = tryLockWrite();
            if (locked)
                super.lock();
            return locked;
        }

        @Override
        public void unlock() {
            super.unlock(); // decrement thread local counter first
            unlockWrite();
        }
    }
}
