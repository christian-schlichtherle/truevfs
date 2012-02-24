/*
 * Copyright (C) 2004-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.ExceptionHandler;
import java.io.IOException;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Uses a JVM shutdown hook to call {@link FsManager#sync} on the decorated
 * file system manager when the JVM terminates.
 * This is to protect an application from loss of data if {@link #sync} isn't
 * called explicitly before the JVM terminates.
 * <p>
 * Note that the shutdown hook is removed before synchronization of the
 * decorated file system manager in order to prevent a potential memory leak
 * if this class is used in multi-classloader-environments, e.g. JEE.
 * In most cases, this class cannot register the JVM shutdown hook again,
 * so your application must repeat calling
 * {@link #sync(BitField, ExceptionHandler)} everytime it has finished
 * processing some changes to some archive files!
 * <p>
 * If any exception occurs within the shutdown hook, its stacktrace is printed
 * to standard error because logging doesn't work in a shutdown hook.
 *
 * @see    #getController(FsMountPoint, FsCompositeDriver)
 * @see    #sync
 * @author Christian Schlichtherle
 */
@ThreadSafe
public final class FsFailSafeManager extends FsDecoratingManager<FsManager> {

    private static final Runtime RUNTIME = Runtime.getRuntime();

    private volatile @CheckForNull Shutdown shutdown;

    public FsFailSafeManager(FsManager manager) {
        super(manager);
    }

    /**
     * {@inheritDoc}
     * <p>
     * If not done before, a shutdown hook is added in order to call
     * {@link FsManager#sync} on the decorated file system manager when the
     * JVM terminates.
     */
    @Override
    public FsController<?>
    getController(  final FsMountPoint mountPoint,
                    final FsCompositeDriver driver) {
        FsController<?> controller = delegate.getController(mountPoint, driver);
        if (null == this.shutdown) { // DCL does work with volatile fields since JSE 5!
            synchronized (this) {
                Shutdown shutdown = this.shutdown;
                if (null == shutdown) {
                    shutdown = new Shutdown(delegate);
                    RUNTIME.addShutdownHook(shutdown);
                    this.shutdown = shutdown;
                }
            }
        }
        return controller;
    }

    /**
     * {@inheritDoc}
     * <p>
     * If a shutdown hook for this manager is present, it's removed before
     * synchronization of the decorated file system manager in order to prevent
     * a potential memory leak if it's used in multi-classloader-environments,
     * e.g. JEE.
     * In most cases, this manager cannot register the JVM shutdown hook
     * again, so your application must repeat this call everytime it has
     * finished processing some changes to some archive files!
     */
    @Override
    public <X extends IOException> void
    sync(   final BitField<FsSyncOption> options,
            final ExceptionHandler<? super IOException, X> handler)
    throws X {
        if (null != this.shutdown) {
            synchronized (this) {
                final Shutdown shutdown = this.shutdown;
                if (null != shutdown) {
                    this.shutdown = null;
                    RUNTIME.removeShutdownHook(shutdown);
                }
            }
        }
        delegate.sync(options, handler);
    }

    /** A shutdown hook thread. */
    private static class Shutdown extends Thread {
        Shutdown(final FsManager manager) {
            super(new Sync(manager), Sync.class.getName());
            super.setPriority(Thread.MAX_PRIORITY);
        }
    } // Shutdown

    /** A runnable which commits all unsynchronized changes to file systems. */
    private static class Sync implements Runnable {
        private final FsManager manager;

        Sync(final FsManager manager) {
            this.manager = manager;
        }

        @Override
        @SuppressWarnings("CallToThreadDumpStack")
        public void run() {
            try {
                manager.sync(FsSyncOptions.UMOUNT);
            } catch (IOException ex) {
                // Logging doesn't work in a shutdown hook!
                ex.printStackTrace();
            }
        }
    } // Sync
}
