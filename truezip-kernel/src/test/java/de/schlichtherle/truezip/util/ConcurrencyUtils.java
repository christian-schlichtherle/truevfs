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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public final class ConcurrencyUtils {

    public static final int
            NUM_IO_THREADS = 10 * Runtime.getRuntime().availableProcessors();

    /** You cannot instantiate this class. */
    private ConcurrencyUtils() { }

    public static AutoCloseable runConcurrent(
            final TaskFactory factory,
            final int nThreads)
    throws InterruptedException {
        final List<Callable<Void>> tasks
                = new ArrayList<Callable<Void>>(nThreads);
        for (int i = 0; i < nThreads; i++)
            tasks.add(factory.newTask(i));

        final List<Future<Void>> results;
        final ExecutorService executor = Executors.newFixedThreadPool(nThreads);
        try {
            // FIXME: This is synchronous!!!
            results = executor.invokeAll(tasks);
        } finally {
            executor.shutdown();
        }
        return new AutoCloseable() {
            @Override
            public void close() throws Exception {
                for (final Future<Void> result : results)
                    result.get(); // check exception from task
            }
        };
    }
}
