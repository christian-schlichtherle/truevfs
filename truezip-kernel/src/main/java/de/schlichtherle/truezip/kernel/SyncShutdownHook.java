/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.kernel;

import de.truezip.kernel.FsManager;
import de.truezip.kernel.FsSyncOptions;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.ThreadSafe;

/**
 * A shutdown hook singleton which {@linkplain FsManager#sync syncs} a
 * {@linkplain SyncShutdownHook#register registered} file system manager when
 * it's run.
 * This is to protect an application from loss of data if the manager isn't
 * explicitly asked to {@code sync()} before the JVM terminates.
 * 
 * @see    FsManager#sync
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class SyncShutdownHook {

    private static final Runtime RUNTIME = Runtime.getRuntime();
    private static final Hook hook = new Hook();

    /** You can't touch this - hammer time! */
    private SyncShutdownHook() { }

    /**
     * Registers the given file system {@code manager} for
     * {@linkplain FsManager#sync synchronization} when the shutdown hook is
     * {@linkplain #run run}.
     * 
     * @param manager the file system manager to
     *        {@linkplain FsManager#sync synchronize} when the shutdown hook
     *        is {@linkplain #run run}.
     * @see   #cancel
     */
    static void register(final FsManager manager) {
        final Hook hook = SyncShutdownHook.hook;
        if (hook.manager != manager) {
            synchronized (hook) {
                if (hook.manager != manager) {
                    RUNTIME.addShutdownHook(hook);
                    hook.manager = manager;
                }
            }
        }
    }

    /**
     * De-registers any previously registered file system manager.
     * 
     * @see #register
     */
    static void cancel() {
        final Hook hook = SyncShutdownHook.hook;
        if (hook.manager != null) {
            synchronized (hook) {
                if (hook.manager != null) {
                    // Prevent memory leak in dynamic class loader environments.
                    RUNTIME.removeShutdownHook(hook);
                    hook.manager = null;
                }
            }
        }
    }

    private static final class Hook extends Thread {
        volatile @CheckForNull FsManager manager;

        private Hook() {
            setPriority(Thread.MAX_PRIORITY);
        }

        /**
         * {@linkplain FsManager#sync Synchronizes} any
         * {@linkplain #register registered} file system manager.
         * <p>
         * If any exception occurs within the shutdown hook, its stacktrace gets
         * printed to standard error because logging doesn't work in a shutdown
         * hook.
         * 
         * @deprecated Do <em>not</em> call this method explicitly!
         * @see #register
         */
        @Override
        @SuppressWarnings(value = "CallToThreadDumpStack")
        public void run() {
            // HC SUNT DRACONES!
            final FsManager manager = this.manager;
            if (manager != null) {
                this.manager = null; // MUST reset to void calls to cancel()!
                try {
                    manager.sync(FsSyncOptions.UMOUNT);
                } catch (final Throwable ex) {
                    // Logging doesn't work in a shutdown hook!
                    ex.printStackTrace();
                }
            }
        }
    }
}
