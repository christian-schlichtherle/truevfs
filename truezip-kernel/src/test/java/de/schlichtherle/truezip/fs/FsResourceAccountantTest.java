/*
 * Copyright (C) 2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs;

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
        final Resource resource = new Resource();
        accountant.startAccountingFor(resource);
        resource.close();
        accountant.startAccountingFor(resource); // redundant
        resource.close();
        accountant.stopAccountingFor(resource);
        resource.close();
        accountant.stopAccountingFor(resource); // redundant
    }

    @Test
    public void testWaitForCurrentThread() throws InterruptedException {
        final Resource resource = new Resource();
        accountant.startAccountingFor(resource);
        long time = System.currentTimeMillis();
        int resources = accountant.waitOtherThreads(TIMEOUT_MILLIS);
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
            int resources = accountant.waitOtherThreads(TIMEOUT_MILLIS);
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
        int resources = accountant.waitOtherThreads(TIMEOUT_MILLIS);
        assertTrue("No timeout!",
                System.currentTimeMillis() >= time + TIMEOUT_MILLIS);
        assertThat(resources, is(2));
        accountant.closeAllResources(SequentialIOExceptionBuilder.create());
        time = System.currentTimeMillis();
        resources = accountant.waitOtherThreads(TIMEOUT_MILLIS);
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
            Resource resource = new Resource();
            accountant.startAccountingFor(resource);
            accountant.startAccountingFor(resource); // redundant call should do no harm
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
            accountant.startAccountingFor(resource);
            accountant.startAccountingFor(resource); // redundant call should do no harm
        }
    } // EvilResourceHog

    private class Resource implements Closeable {
        @Override
        public void close() {
        }
    } // Resource
}
