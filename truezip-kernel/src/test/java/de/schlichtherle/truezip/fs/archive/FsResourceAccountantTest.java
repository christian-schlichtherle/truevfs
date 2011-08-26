/*
 * Copyright (C) 2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.fs.archive;

import de.schlichtherle.truezip.io.SequentialIOExceptionBuilder;
import org.junit.After;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.Closeable;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
public class FsResourceAccountantTest {

    private static long TIMEOUT_MILLIS = 100;

    private Lock lock;
    private FsResourceAccountant accountant;

    @Before
    public void setUp() {
        this.lock = new ReentrantLock();
        this.accountant = new FsResourceAccountant(lock);
    }

    @After
    public void tearDown() {
        System.gc();
    }

    @Test
    public void testAccounting() throws IOException {
        final Resource resource = new AccountingResource();
        resource.close();

        assertTrue(accountant.startAccountingFor(resource));
        assertThat(resource.getCloseCounter(), is(1));
        resource.close();
        assertThat(resource.getCloseCounter(), is(2));
        assertFalse(accountant.stopAccountingFor(resource));
        resource.close();
        assertFalse(accountant.stopAccountingFor(resource));
        assertThat(resource.getCloseCounter(), is(2));
    }

    @Test
    public void testWaitForCurrentThread() throws InterruptedException {
        final Resource resource = new Resource();
        assertTrue(accountant.startAccountingFor(resource));
        long time = System.currentTimeMillis();
        int resources = accountant.waitStopAccounting(TIMEOUT_MILLIS);
        assertTrue("Timeout!", System.currentTimeMillis() < time + TIMEOUT_MILLIS);
        assertThat(resources, is(1));
    }

    @Test
    public void testWaitForOtherThreads() throws InterruptedException {
        final Thread[] threads = new Thread[] {
            new ResourceHog(),
            new EvilResourceHog(),
        };
        for (int i = 0; i < threads.length; i++) {
            final Class<?> clazz = threads[i].getClass();
            threads[i].start();
            threads[i].join();
            threads[i] = null;
            System.gc();
            long time = System.currentTimeMillis();
            int resources = accountant.waitStopAccounting(TIMEOUT_MILLIS);
            assertTrue("Timeout waiting for " + clazz.getName(),
                    System.currentTimeMillis() < time + TIMEOUT_MILLIS);
            assertThat(resources, is(0));
        }
    }

    @Test
    public void testCloseAll() throws IOException, InterruptedException {
        final Thread[] threads = new Thread[] {
            new ResourceHog(),
            new EvilResourceHog(),
        };
        for (Thread thread : threads) {
            thread.start();
            thread.join();
        }
        long time = System.currentTimeMillis();
        int resources = accountant.waitStopAccounting(TIMEOUT_MILLIS);
        assertTrue("No timeout!",
                System.currentTimeMillis() >= time + TIMEOUT_MILLIS);
        assertThat(resources, is(2));
        accountant.closeAll(SequentialIOExceptionBuilder.create());
        time = System.currentTimeMillis();
        resources = accountant.waitStopAccounting(TIMEOUT_MILLIS);
        assertTrue("Timeout!",
                System.currentTimeMillis() < time + TIMEOUT_MILLIS);
        assertThat(resources, is(0));
    }

    /**
     * A resource hog is a thread which starts accounting for a resource, but
     * never stops accounting for it.
     * We want to make sure that the TrueZIP resource collector picks up the
     * stale resource then.
     */
    private final class ResourceHog extends Thread {
        @Override
        public void run() {
            assertTrue(accountant.startAccountingFor(new Resource()));
        }
    } // ResourceHog

    /**
     * An evil resource hog is a thread which starts accounting for a resource,
     * but never stops accounting for it <em>and</em> keeps a strong reference
     * to it.
     * We want to make sure that the TrueZIP resource collector picks up the
     * stale resource then.
     */
    private final class EvilResourceHog extends Thread {
        final Resource resource = new Resource();

        @Override
        public void run() {
            assertTrue(accountant.startAccountingFor(resource));
        }
    } // EvilResourceHog

    private class Resource implements Closeable {
        volatile int closeCounter;

        final int getCloseCounter() {
            return closeCounter;
        }

        @Override
        public void close() throws IOException {
            FsResourceAccountantTest.this.lock.lock();
            try {
                closeCounter++;
            } finally {
                FsResourceAccountantTest.this.lock.unlock();
            }
        }
    } // Resource

    private final class AccountingResource extends Resource {
        @SuppressWarnings("LeakingThisInConstructor")
        AccountingResource() {
            assertTrue(accountant.startAccountingFor(this));
            assertFalse(accountant.startAccountingFor(this));
        }

        @Override
        public void close() throws IOException {
            if (accountant.stopAccountingFor(this))
                super.close();
            assertFalse(accountant.stopAccountingFor(this));
        }
    } // AccountingResource
}
