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
 * but provides read/write locks with a slightly different interface in order
 * to suit the particular needs of TrueZIP (see {@link ReentrantLock}).
 * Methods common to both APIs have identical contracts in order not to
 * confuse users.
 * <p>
 * <b>Note:</b> In accordance with JSE 5, any attempt to upgrade a read lock
 * to a write will lock the current thread.
 * This is by design: If this limitation would not exist, two reader threads
 * could try to upgrade from a read lock to a write lock concurrently,
 * effectively dead locking each other.
 * By locking this thread immediately on any attempt to do so, this is
 * considered to be a programming error which can be easily fixed without
 * affecting any other thread.
 * <p>
 * Conversely, it's possible to downgrade from a write lock to a read lock.
 * Please consult the JSE 5 Javadoc of the class
 * {@code java.util.concurrent.locks.ReentrantReadWriteLock} for more
 * information.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public final class ReentrantReadWriteLock implements ReadWriteLock {

    private static final String CLASS_NAME
            = "de.schlichtherle.truezip.util.concurrent.locks.ReentrantReadWriteLock";
    private static final Logger logger
            = Logger.getLogger(CLASS_NAME, CLASS_NAME);

    private int  readCount; // shared
    private int writeCount; // exclusive

    private final ReadLock readLock = new ReadLock();
    private final WriteLock writeLock = new WriteLock();

    /**
     * Returns the lock for reading.
     * Like its cousin in JSE 1.5, the returned lock does <em>not</em>
     * support upgrading to a write lock.
     */
    @Override
	public ReentrantLock readLock() {
        return readLock;
    }

    /**
     * Returns the lock for writing.
     * Like its cousin in JSE 5, the returned lock <em>does</em>
     * support downgrading to a read lock.
     */
    @Override
	public ReentrantLock writeLock() {
        return writeLock;
    }

    /**
     * Runs the given operation while the write lock is temporarily acquired
     * even if the read lock is already acquired by the current thread.
     * <p>
     * <b>Warning:</b> This method temporarily releases the read lock
     * before the write lock is temporarily acquired and the operation is run!
     * Hence, the operation must recheck the preconditions for running it
     * before it proceeds with the tasks which require the write lock.
     * <p>
     * Upon return, the hold count of the read and write lock for the current
     * thread is fully restored, even if the operation throws an exception.
     *
     * @param  operation a non-{@code null} operation to run while the write
     *         lock is acquired.
     * @throws NullPointerException if {@code operation} is {@code null}.
     * @throws Exception upon the discretion of {@code operation}.
     * @return {@code operation}
     */
    public <E extends Exception, O extends Operation<E>> O runWriteLocked(
            final O operation)
    throws E {
        if (operation == null)
            throw new NullPointerException();

        if (writeLock.isHeldByCurrentThread()) {
            // Calls to *.unlock/lock() are redundant.
            operation.run();
        } else {
            // A read lock cannot get upgraded to a write lock.
            // Hence the following mess is required.
            // Note that this is not just a limitation of the current
            // implementation in JSE 5: If automatic upgrading were implemented,
            // two threads holding a read lock try to upgrade concurrently,
            // they would dead lock each other!
            final int readHoldCount = readLock.getHoldCount();
            for (int c = readHoldCount; c > 0; c--)
                readLock.unlock();

            // Current thread may get blocked exactly here!
            writeLock.lock();
            try {
                for (int c = readHoldCount; c-- > 0; )
                    readLock.lock();
                operation.run(); // beware of side effects on locks!
            } finally {
                writeLock.unlock();
            }
        }
        return operation;
    }

    private void lockRead() {
        final int writeHoldCount = writeLock.getHoldCount();
        synchronized (this) {
            // Wait until no other writer has acquired a lock.
            while (writeCount > writeHoldCount) {
                assert writeHoldCount == 0 : "write lock isn't held exclusively!";
                try {
                    wait();
                } catch (InterruptedException ex) {
                    logger.log(Level.FINE, "interrupted", ex);
                    logger.log(Level.FINE, "continuing");
                }
            }
            assert writeCount == writeHoldCount : "write lock/hold mismatch!";
            readCount++;
        }
    }

    private void lockInterruptiblyRead()
    throws InterruptedException {
        final int writeHoldCount = writeLock.getHoldCount();
        synchronized (this) {
            // Wait until no other writer has acquired a lock.
            while (writeCount > writeHoldCount) {
                assert writeHoldCount == 0 : "write lock isn't held exclusively!";
                wait();
            }
            assert writeCount == writeHoldCount : "write lock/hold mismatch!";
            readCount++;
        }
    }

    private boolean tryLockRead() {
        final int writeHoldCount = writeLock.getHoldCount();
        synchronized (this) {
            // Check if another writer has acquired a lock.
            if (writeCount > writeHoldCount) {
                assert writeHoldCount == 0 : "write lock isn't held exclusively!";
                return false;
            }
            assert writeCount == writeHoldCount : "write lock/hold mismatch!";
            readCount++;
        }
        return true;
    }

    private synchronized void unlockRead() {
        readCount--;
        notifyAll();
    }

    private void lockWrite() {
        final int writeHoldCount = writeLock.getHoldCount();
        synchronized (this) {
            if (writeHoldCount <= 0) { // If I'm not the writer...
                final int readHoldCount = readLock.getHoldCount();
                // ... wait until no other writer and no readers have acquired a lock.
                while (readCount > 0 /*- readHoldCount*/ // mimic JSE 5: dead lock on lock upgrade!
                        || writeCount > writeHoldCount) {
                    try {
                        wait();
                    } catch (InterruptedException ex) {
                        logger.log(Level.FINE, "interrupted", ex);
                        logger.log(Level.FINE, "continuing");
                    }
                }
                assert readCount == readHoldCount : "read lock/hold mismatch!";
            }
            writeCount++;
        }
    }

    private void lockInterruptiblyWrite()
    throws InterruptedException {
        final int writeHoldCount = writeLock.getHoldCount();
        synchronized (this) {
            if (writeHoldCount <= 0) { // If I'm not the writer...
                final int readHoldCount = readLock.getHoldCount();
                // ... wait until no other writer and no readers have acquired a lock.
                while (readCount > 0 /* readHoldCount*/ // mimic JSE 5: dead lock on lock upgrade!
                        || writeCount > writeHoldCount) {
                    wait();
                }
                assert readCount == readHoldCount : "read lock/hold mismatch!";
            }
            writeCount++;
        }
    }

    private boolean tryLockWrite() {
        final int writeHoldCount = writeLock.getHoldCount();
        synchronized (this) {
            if (writeHoldCount <= 0) { // If I'm not the writer...
                final int readHoldCount = readLock.getHoldCount();
                // ... check if another reader or writer has acquired a lock.
                if (readCount > 0 /* readHoldCount*/ // mimic JSE 5: dead lock on lock upgrade!
                        || writeCount > writeHoldCount) {
                    return false;
                }
                assert readCount == readHoldCount : "read lock/hold mismatch!";
            }
            writeCount++;
        }
        return true;
    }

    private synchronized void unlockWrite() {
        writeCount--;
        notifyAll();
    }

    private static abstract class AbstractLock
            extends ThreadLocal<Integer>
            implements ReentrantLock {

        @Override
        protected Integer initialValue() {
            return 0;
        }

        @Override
		public final boolean isHeldByCurrentThread() {
            return get() > 0;
        }

        @Override
		public final int getHoldCount() {
            return get();
        }

        @Override
		public void lock() {
            set(get() + 1);
        }

        @Override
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

        @Override
		public void lockInterruptibly() throws InterruptedException {
            lockInterruptiblyRead();
            super.lock(); // increment thread local counter last
        }

        @Override
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

        @Override
		public void lockInterruptibly() throws InterruptedException {
            lockInterruptiblyWrite();
            super.lock(); // increment thread local counter last
        }

        @Override
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
