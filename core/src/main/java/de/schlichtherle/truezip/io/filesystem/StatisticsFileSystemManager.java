/*
 * Copyright (C) 2010 Schlichtherle IT Services
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
package de.schlichtherle.truezip.io.filesystem;

import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.ExceptionHandler;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import net.jcip.annotations.ThreadSafe;

/**
 * Provides statistics for the federated file systems managed by the decorated
 * file system manager.
 *
 * @author  Christian Schlichtherle
 * @version $Id: StatisticsFileSystemManager.java,v de8626cc844d 2010/12/20 21:21:08 christian $
 */
@ThreadSafe
public final class StatisticsFileSystemManager
extends DecoratingFileSystemManager<FileSystemManager> {

    private volatile FileSystemStatistics statistics
            = new FileSystemStatistics(this);

    /**
     * Constructs a new statistics file system manager.
     *
     * @param manager the decorated file system manager.
     */
    public StatisticsFileSystemManager(@NonNull FileSystemManager manager) {
        super(manager);
    }

    @Override
    public FileSystemController<?> getController(
            final MountPoint mountPoint,
            final FileSystemDriver<?> driver) {

        class Driver implements FileSystemDriver<FileSystemModel> {
            @Override
            public FileSystemController<?> newController(
                    MountPoint mountPoint,
                    FileSystemController<?> parent) {
                final FileSystemController<?> controller
                        = driver.newController(mountPoint, parent);
                return null != parent && null == parent.getParent() // controller is top level federated file system?
                        ? new StatisticsFileSystemController(controller, StatisticsFileSystemManager.this)
                        : controller;
            }
        } // class Driver

        return delegate.getController(mountPoint, new Driver());
    }

    /**
     * Returns statistics about the set of federated file systems managed by
     * the decorated file system manager.
     * The statistics provided by the returned object get asynchronously
     * updated up to the next call to {@link #sync}.
     * <p>
     * Note that there may be a slight delay until the values returned reflect
     * the actual state of this package.
     * This delay increases if the system is under heavy load.
     *
     * @see #sync
     * @see FileSystemStatistics#isClosed
     */
    @NonNull
    public FileSystemStatistics getStatistics() {
        return statistics;
    }

    /**
     * {@inheritDoc}
     * <p>
     * After the synchronization, this implementation creates a new statistics
     * object to be returned by a subsequent call to {@link #getStatistics}.
     */
    @Override
    public <E extends IOException>
    void sync(  BitField<SyncOption> options,
                ExceptionHandler<? super IOException, E> handler)
    throws E {
        try {
            super.sync(options, handler);
        } finally {
            statistics = new FileSystemStatistics(this);
        }
    }
}
