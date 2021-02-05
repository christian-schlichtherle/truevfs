/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.commons.shed;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * @author Christian Schlichtherle
 */
public class ConcurrencyUtils {

    /**
     * The number of threads for doing CPU intensive tasks considering 0%
     * blocking factor.
     */
    public static final int NUM_CPU_THREADS =
            Runtime.getRuntime().availableProcessors();

    /**
     * The number of threads for doing I/O intensive tasks considering 90%
     * blocking factor.
     */
    public static final int NUM_IO_THREADS = 10 * NUM_CPU_THREADS;

    private ConcurrencyUtils() { }

    public static TaskJoiner start(
            final int numThreads,
            final TaskFactory factory) {
        final CountDownLatch latch = new CountDownLatch(numThreads);
        final List<Future<?>> results = new ArrayList<>(numThreads);
        final ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        try {
            for (int threadNum = 0; threadNum < numThreads; threadNum++) {
                final Callable<?> task = factory.newTask(threadNum);

                final class Starter implements Callable<Void> {
                    @Override
                    public Void call() throws Exception {
                        latch.countDown();
                        task.call();
                        return null;
                    }
                }

                results.add(executor.submit(new Starter()));
            }
        } finally {
            executor.shutdown();
        }

        final class TaskJoinerImpl implements TaskJoiner {

            @Override
            public void cancel() {
                //executor.shutdownNow(); // TODO: Explain why this doesn't work sometimes!
                for (final Future<?> result : results)
                    result.cancel(true);
            }

            @Override
            public void join() throws InterruptedException, ExecutionException {
                final SuppressedExceptionBuilder<ExecutionException>
                        builder = new SuppressedExceptionBuilder<>();
                for (final Future<?> result : results) {
                    try {
                        result.get(); // check exception from task
                    } catch (ExecutionException ex) {
                        builder.warn(ex);
                    } catch (CancellationException ignored) {
                    }
                }
                builder.check();
            }
        }

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
