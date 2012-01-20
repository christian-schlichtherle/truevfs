/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public class ConcurrencyUtils {

    public static final int
            NUM_IO_THREADS = 10 * Runtime.getRuntime().availableProcessors();

    /** You cannot instantiate this class. */
    private ConcurrencyUtils() { }

    public static TaskJoiner runConcurrent(
            final int nThreads,
            final TaskFactory factory) {
        final List<Future<Void>> results
                = new ArrayList<Future<Void>>(nThreads);
        final ExecutorService executor = Executors.newFixedThreadPool(nThreads);
        try {
            for (int i = 0; i < nThreads; i++)
                results.add(executor.submit(factory.newTask(i)));
        } finally {
            executor.shutdown();
        }
        return new TaskJoiner() {
            @Override
            public void join() throws InterruptedException, ExecutionException {
                try {
                    for (final Future<Void> result : results)
                        result.get(); // check exception from task
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } finally {
                    executor.awaitTermination(0, TimeUnit.DAYS);
                }
            }
        };
    }

    public interface TaskFactory {
        Callable<Void> newTask(int threadNum);
    }

    public interface TaskJoiner {
        public void join() throws InterruptedException, ExecutionException;
    }
}
