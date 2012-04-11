/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.kernel;

import de.truezip.kernel.*;
import de.truezip.kernel.FsMountPoint;
import de.truezip.kernel.FsSyncOption;
import de.truezip.kernel.FsSyncOptions;
import de.truezip.kernel.util.BitField;
import de.truezip.kernel.util.ExceptionHandler;
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
final class FailSafeManager extends FsDecoratingManager<FsManager> {

    private static final Runtime RUNTIME = Runtime.getRuntime();

    private volatile @CheckForNull ShutdownHook shutdownHook;

    FailSafeManager(FsManager manager) {
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
        FsController<?> controller = manager.getController(mountPoint, driver);
        if (null == this.shutdownHook) { // DCL does work with volatile fields since JSE 5!
            synchronized (this) {
                ShutdownHook shutdown = this.shutdownHook;
                if (null == shutdown) {
                    shutdown = new ShutdownHook(manager);
                    RUNTIME.addShutdownHook(shutdown);
                    this.shutdownHook = shutdown;
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
    public void
    sync(   final BitField<FsSyncOption> options,
            final ExceptionHandler<? super FsSyncException, ? extends FsSyncException> handler)
    throws FsSyncWarningException, FsSyncException {
        if (null != this.shutdownHook) {
            synchronized (this) {
                final ShutdownHook shutdownHook = this.shutdownHook;
                if (null != shutdownHook) {
                    this.shutdownHook = null;
                    RUNTIME.removeShutdownHook(shutdownHook);
                }
            }
        }
        manager.sync(options, handler);
    }

    /** A shutdown hook thread. */
    private static class ShutdownHook extends Thread {
        private final FsManager manager;

        ShutdownHook(final FsManager manager) {
            super(ShutdownHook.class.getName());
            super.setPriority(Thread.MAX_PRIORITY);
            this.manager = manager;
        }

        @Override
        @SuppressWarnings("CallToThreadDumpStack")
        public void run() {
            try {
                manager.sync(FsSyncOptions.UMOUNT);
            } catch (final IOException ex) {
                // Logging doesn't work in a shutdown hook!
                ex.printStackTrace();
            }
        }
    } // ShutdownHook
}
