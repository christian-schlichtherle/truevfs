/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * @author Christian Schlichtherle
 */
public class ConcurrencyUtils {

    /** The number of threads for doing I/O considering 90% blocking factor. */
    public static final int
            NUM_IO_THREADS = 10 * Runtime.getRuntime().availableProcessors();

    /** You cannot instantiate this class. */
    private ConcurrencyUtils() { }

    public static TaskJoiner runConcurrent(
            final int numThreads,
            final TaskFactory factory) {
        final List<Future<?>> results = new ArrayList<Future<?>>(numThreads);
        final ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        try {
            for (int threadNum = 0; threadNum < numThreads; threadNum++)
                results.add(executor.submit(factory.newTask(threadNum)));
        } finally {
            executor.shutdown();
        }

        class TaskJoinerImpl implements TaskJoiner {
            @Override
            public void cancel() {
                //executor.shutdownNow(); // TODO: Explain why this doessn't work sometimes!
                for (final Future<?> result : results)
                    result.cancel(true);
            }

            @Override
            public void join() throws InterruptedException, ExecutionException {
                ExecutionException failure = null;
                for (final Future<?> result : results) {
                    try {
                        result.get(); // check exception from task
                    } catch (ExecutionException failed) {
                        failure = failed;
                    } catch (CancellationException cancelled) {
                    }
                }
                if (null != failure)
                    throw failure;
            }
        } // TaskJoinerImpl

        return new TaskJoinerImpl();
    }

    @SuppressWarnings("PublicInnerClass")
    public interface TaskFactory {
        Callable<?> newTask(int threadNum);
    }

    @SuppressWarnings("PublicInnerClass")
    public interface TaskJoiner {
        public void cancel();
        public void join() throws InterruptedException, ExecutionException;
    }
}
