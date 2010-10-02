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
import junit.framework.TestCase;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class ReentrantReadWriteLockTest extends TestCase {

    public ReentrantReadWriteLockTest(String testName) {
        super(testName);
    }

    private ReentrantReadWriteLock instance;

    @Override
    protected void setUp() throws Exception {
        instance = new ReentrantReadWriteLock();
    }

    @Override
    protected void tearDown() throws Exception {
        instance = null;
    }

    /**
     * Test of readLock method, of class de.schlichtherle.truezip.util.concurrent.locks.ReentrantReadWriteLock.
     */
    public void testReadLock() {
        ReentrantLock rl = instance.readLock();
        assertNotNull(rl);

        rl.lock();
        try {
            runWithTimeout(1000, new Operation<InterruptedException>() {
                public void run() throws InterruptedException {
                    ReentrantLock wl = instance.writeLock();
                    // Upgrading a read lock blocks until interrupted.
                    wl.lockInterruptibly();
                }
            });
            fail("Upgrading a read lock should dead lock!");
        } catch (InterruptedException expected) {
        }
    }

    /**
     * Test of writeLock method, of class de.schlichtherle.truezip.util.concurrent.locks.ReentrantReadWriteLock.
     */
    public void testWriteLock() throws InterruptedException {
        ReentrantLock wl = instance.writeLock();
        assertNotNull(wl);

        wl.lock();
        runWithTimeout(1000, new Operation<InterruptedException>() {
            public void run() throws InterruptedException {
                ReentrantLock rl = instance.readLock();
                // Downgrading a write lock returns immediately.
                rl.lockInterruptibly();
            }
        });
    }

    private <T extends Exception> void runWithTimeout(
            final long timeout,
            final Operation<T> action)
    throws T {
        final Thread target = Thread.currentThread();
        final Thread observer = new Thread(new Runnable() {
            @SuppressWarnings("CallToThreadDumpStack")
            public void run() {
                try {
                    Thread.sleep(timeout);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                target.interrupt();
            }
        }, "Timeout thread");
        observer.setDaemon(true);
        observer.start();
        action.run();
    }
}
