/*
 * Copyright (C) 2011 Schlichtherle IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.schlichtherle.truezip.fs;

import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.ExceptionHandler;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import net.jcip.annotations.ThreadSafe;

/**
 * Uses a JVM shutdown hook to call {@link FsManager#sync} on the decorated
 * file system manager when the JVM terminates.
 * This is to protect an application from loss of data if {@link #sync} isn't
 * called explicitly before the JVM terminates.
 * <p>
 * If any exception occurs within the shutdown hook, its stacktrace is printed
 * to standard error - logging doesn't work in a shutdown hook.
 *
 * @see     #getController(FsMountPoint, FsCompositeDriver)
 * @see     #sync
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public final class FsFailSafeManager extends FsDecoratingManager<FsManager> {

    private static final Runtime RUNTIME = Runtime.getRuntime();

    private volatile Shutdown shutdown;

    public FsFailSafeManager(@NonNull FsManager manager) {
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
    getController(FsMountPoint mountPoint, FsCompositeDriver driver) {
        FsController<?> controller = delegate.getController(mountPoint, driver);
        if (null == this.shutdown) { // DCL does work with volatile fields since JSE 5!
            synchronized (this) {
                Shutdown shutdown = this.shutdown;
                if (null == shutdown) {
                    shutdown = new Shutdown(new Sync(delegate));
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
     * synchronization of the decorated file system manager.
     */
    @Override
    public <X extends IOException> void
    sync(   BitField<FsSyncOption> options,
            ExceptionHandler<? super IOException, X> handler)
    throws X {
        if (null != this.shutdown) {
            synchronized (this) {
                Shutdown shutdown = this.shutdown;
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
        Shutdown(Runnable runnable) {
            super(  runnable,
                    "TrueZIP FileSystemManager Shutdown Hook");
            super.setPriority(Thread.MAX_PRIORITY);
        }
    } // class Shutdown

    /** A runnable which committs all unsynchronized changes to file systems. */
    private static class Sync implements Runnable {
        private final FsManager manager;

        Sync(final FsManager manager) {
            assert null != manager;
            this.manager = manager;
        }

        @Override
        @SuppressWarnings("CallToThreadDumpStack")
        public void run() {
            try {
                manager.sync(UMOUNT);
            } catch (IOException ex) {
                // Logging doesn't work in a shutdown hook!
                ex.printStackTrace();
            }
        }
    } // class Sync
}

