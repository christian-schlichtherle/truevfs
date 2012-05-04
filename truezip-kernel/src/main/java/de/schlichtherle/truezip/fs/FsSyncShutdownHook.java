/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

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
final class FsSyncShutdownHook extends Thread {
    private static final Runtime RUNTIME = Runtime.getRuntime();
    static final FsSyncShutdownHook SINGLETON = new FsSyncShutdownHook();

    private volatile @CheckForNull FsManager manager;

    private FsSyncShutdownHook() {
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
            try {
                manager.sync(FsSyncOptions.UMOUNT);
            } catch (final Throwable ex) {
                // Logging doesn't work in a shutdown hook!
                ex.printStackTrace();
            }
        }
    }

    /**
     * Registers the given file system {@code manager} for
     * {@linkplain FsManager#sync synchronization} when this shutdown hook is
     * {@linkplain #run run}.
     * 
     * @param manager the file system manager to
     *        {@linkplain FsManager#sync synchronize} when this shutdown hook
     *        is {@linkplain #run run}.
     * @see   #cancel
     */
    void register(final FsManager manager) {
        if (this.manager != manager) {
            synchronized (this) {
                if (this.manager != manager) {
                    RUNTIME.addShutdownHook(this);
                    this.manager = manager;
                }
            }
        }
    }

    /**
     * De-registers any previously registered file system manager.
     * 
     * @see #register
     */
    void cancel() {
        if (manager != null) {
            synchronized (this) {
                if (manager != null) {
                    // Prevent memory leak in dynamic class loader environments.
                    RUNTIME.removeShutdownHook(this);
                    manager = null;
                }
            }
        }
    }
}
