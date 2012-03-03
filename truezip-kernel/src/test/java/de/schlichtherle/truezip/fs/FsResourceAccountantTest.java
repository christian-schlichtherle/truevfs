/*
 * Copyright (C) 2004-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import de.schlichtherle.truezip.io.SequentialIOExceptionBuilder;
import de.schlichtherle.truezip.util.ConcurrencyUtils.TaskFactory;
import de.schlichtherle.truezip.util.ConcurrencyUtils.TaskJoiner;
import static de.schlichtherle.truezip.util.ConcurrencyUtils.runConcurrent;
import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * @author Christian Schlichtherle
 */
public class FsResourceAccountantTest {

    /** The waiting timeout in milliseconds, which is {@code value}. */
    private static final long TIMEOUT_MILLIS = 100;

    private static final Logger
            logger = Logger.getLogger(FsResourceAccountantTest.class.getName());

    private final FsResourceAccountant
            accountant = new FsResourceAccountant(new ReentrantLock());

    @Test
    public void testAccounting() throws IOException {
        final EvilResource resource = new EvilResource();
        accountant.startAccountingFor(resource);
        accountant.startAccountingFor(resource); // redundant
        accountant.stopAccountingFor(resource);
        accountant.stopAccountingFor(resource); // redundant
    }

    @Test
    public void testMultithreadedAccounting()
    throws InterruptedException, ExecutionException {

        class ResourceHogFactory implements TaskFactory {
            @Override
            public Callable<?> newTask(int threadNum) {
                return new ResourceHog();
            }
        } // ResourceHogFactory

        final TaskJoiner join = runConcurrent(100, new ResourceHogFactory());
        try {
            waitAllResources();
        } finally {
            join.join();
        }
    }

    private void waitAllResources() {
        final long start = System.currentTimeMillis();
        do {
            System.gc(); // triggering GC in a loop seems to help with concurrency!
        } while (0 < accountant.waitForeignResources(TIMEOUT_MILLIS));
        final long time = System.currentTimeMillis() - start;
        logger.log(Level.FINER, "All resources were closed after waiting for {0} ms.", time);
    }

    @Test
    public void testWaitLocalResources() throws InterruptedException {
        final EvilResource resource = new EvilResource();
        accountant.startAccountingFor(resource);
        Thread.currentThread().interrupt();
        final long start = System.currentTimeMillis();
        final int resources = accountant.waitForeignResources(TIMEOUT_MILLIS);
        final long time = System.currentTimeMillis() - start;
        assertTrue("Unexpected timeout after " + time + " ms!",
                time < TIMEOUT_MILLIS); // should be close to zero
        assertTrue(Thread.interrupted()); // clear interrupt status!
        assertThat(resources, is(1));
    }

    @Test
    public void testWaitForeignResources() throws InterruptedException {
        final Thread[] threads = new Thread[] {
            new ResourceHog(),
            new EvilResourceHog(),
        };
        for (int i = 0; i < threads.length; i++) {
            final Class<?> clazz = threads[i].getClass();
            threads[i].start();
            threads[i].join();
            threads[i] = null;
            waitAllResources();
            Thread.currentThread().interrupt();
            final long start = System.currentTimeMillis();
            int resources = accountant.waitForeignResources(TIMEOUT_MILLIS);
            final long time = System.currentTimeMillis() - start;
            assertTrue("Unexpected timeout while waiting for " + clazz.getSimpleName() + " after " + time + " ms!",
                    time < TIMEOUT_MILLIS); // should be close to zero
            assertTrue(Thread.interrupted()); // clear interrupt status!
            assertThat(resources, is(0));
        }
    }

    @Test
    public void testCloseAllResources() throws IOException, InterruptedException {
        final Thread[] threads = new Thread[] {
            new ResourceHog(),
            new EvilResourceHog(),
        };
        for (final Thread thread : threads) {
            thread.start();
            thread.join();
        }
        System.gc();
        Thread.sleep(TIMEOUT_MILLIS);

        long start, time;
        int resources;

        Thread.currentThread().interrupt();
        start = System.currentTimeMillis();
        resources = accountant.waitForeignResources(TIMEOUT_MILLIS);
        time = System.currentTimeMillis() - start;
        assertTrue("Unexpected timeout after " + time + " ms!",
                time < TIMEOUT_MILLIS); // should be close to zero
        assertFalse(Thread.interrupted()); // clear interrupt status anyway!
        assertTrue(resources > 0);

        start = System.currentTimeMillis();
        resources = accountant.waitForeignResources(TIMEOUT_MILLIS);
        time = System.currentTimeMillis() - start;
        assertTrue("Premature return from waiting for " + resources + " open resources after " + time + " ms instead of expected timeout after " + TIMEOUT_MILLIS + " ms!",
                time >= TIMEOUT_MILLIS);
        assertTrue(resources > 0);

        accountant.closeAllResources(SequentialIOExceptionBuilder.create());

        start = System.currentTimeMillis();
        resources = accountant.waitForeignResources(TIMEOUT_MILLIS);
        time = System.currentTimeMillis() - start;
        assertTrue("Unexpected timeout after " + time + " ms!",
                time < TIMEOUT_MILLIS); // should be close to zero
        assertThat(resources, is(0));
    }

    /**
     * A resource hog is a thread which starts accounting for a resource, but
     * never stops accounting for it.
     * We want to make sure that the TrueZIP resource collector picks up the
     * stale resource then.
     */
    private final class ResourceHog extends Thread implements Callable<Void> {
        @Override
        public void run() {
            EvilResource resource = new EvilResource();
            accountant.startAccountingFor(resource);
            accountant.startAccountingFor(resource); // redundant call should do no harm
        }

        @Override
        @SuppressWarnings("CallToThreadRun")
        public Void call() {
            run();
            return null;
        }
    } // ResourceHog

    /**
     * An evil resource hog is a thread which starts accounting for a resource,
     * but never stops accounting for it <em>and</em> keeps a strong reference
     * to it.
     * We want to make sure that the TrueZIP resource collector picks up the
     * stale resource then.
     */
    private final class EvilResourceHog extends Thread implements Callable<Void> {
        final EvilResource resource = new EvilResource();

        @Override
        public void run() {
            accountant.startAccountingFor(resource);
            accountant.startAccountingFor(resource); // redundant call should do no harm
        }

        @Override
        @SuppressWarnings("CallToThreadRun")
        public Void call() {
            run();
            return null;
        }
    } // EvilResourceHog

    private class EvilResource implements Closeable {
        @Override
        public boolean equals(Object obj) {
            return obj instanceof EvilResource; // worst case
        }

        @Override
        public int hashCode() {
            return 0; // worst case
        }
        
        @Override
        public void close() {
        }
    } // Resource
}
