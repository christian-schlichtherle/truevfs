/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public class FsResourceAccountantTest {

    /** The waiting timeout in milliseconds, which is {@code value}. */
    private static final long TIMEOUT_MILLIS = 100;

    private static final Logger
            logger = Logger.getLogger(FsResourceAccountantTest.class.getName());

    private final FsResourceAccountant
            accountant = new FsResourceAccountant(new ReentrantLock());

    @Test
    public void accounting() throws IOException {
        final Resource resource = new Resource();
        accountant.startAccountingFor(resource);
        accountant.startAccountingFor(resource); // redundant
        accountant.stopAccountingFor(resource);
        accountant.stopAccountingFor(resource); // redundant
    }

    @Test
    public void multithreadedAccounting()
    throws InterruptedException, ExecutionException {

        class ResourceHogFactory implements TaskFactory {
            @Override
            public Callable<Void> newTask(int threadNum) {
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
        logger.log(Level.FINER, "All resources were closed after waiting for {0} milliseconds.", time);
    }

    @Test
    public void waitLocalResources() throws InterruptedException {
        final Resource resource = new Resource();
        accountant.startAccountingFor(resource);
        final long start = System.currentTimeMillis();
        final int resources = accountant.waitForeignResources(TIMEOUT_MILLIS);
        final long time = System.currentTimeMillis() - start;
        assertTrue("Timeout after " + time + " milliseconds!",
                time <= TIMEOUT_MILLIS); // be forgiving!
        assertThat(resources, is(1));
    }

    @Test
    public void waitForeignResources() throws InterruptedException {
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
            final long start = System.currentTimeMillis();
            int resources = accountant.waitForeignResources(TIMEOUT_MILLIS);
            final long time = System.currentTimeMillis() - start;
            assertTrue("Timeout while waiting for " + clazz.getSimpleName() + " after " + time + " milliseconds!",
                    time <= TIMEOUT_MILLIS); // be forgiving!
            assertThat(resources, is(0));
        }
    }

    @Test
    public void closeAllResources() throws IOException, InterruptedException {
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
        long start = System.currentTimeMillis();
        int resources = accountant.waitForeignResources(TIMEOUT_MILLIS);
        long time = System.currentTimeMillis() - start;
        assertTrue("Premature return before timeout after " + time + " milliseconds with " + resources + " open resources!",
                time >= TIMEOUT_MILLIS); // be forgiving!
        assertTrue(resources > 0);
        accountant.closeAllResources(SequentialIOExceptionBuilder.create());
        start = System.currentTimeMillis();
        resources = accountant.waitForeignResources(TIMEOUT_MILLIS);
        time = System.currentTimeMillis() - start;
        assertTrue("Timeout after " + time + " milliseconds!",
                time <= TIMEOUT_MILLIS); // be forgiving!
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
            Resource resource = new Resource();
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
        final Resource resource = new Resource();

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

    private class Resource implements Closeable {
        @Override
        public void close() {
        }
    } // Resource
}
