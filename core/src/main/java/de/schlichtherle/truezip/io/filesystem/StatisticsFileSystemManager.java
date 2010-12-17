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
import de.schlichtherle.truezip.util.ExceptionBuilder;
import java.io.IOException;
import java.util.Set;

/**
 * Provides statistics for the federated file systems managed by the instances
 * of this class.
 * <p>
 * Note that this class is thread-safe.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public final class StatisticsFileSystemManager
extends FileSystemManager {

    @Override
    public FileSystemController<?> getController(
            final MountPoint mountPoint,
            final FileSystemDriver<?> driver,
            FileSystemController<?> parent) {
        final FileSystemController<?> controller
                = super.getController(mountPoint, driver, parent);
        parent = controller.getParent();
        return null != parent && null == parent.getParent() // controller is top level federated file system?
                ? new StatisticsFileSystemController(controller, this)
                : controller;
    }

    private ManagedFileSystemStatistics statistics
            = new ManagedFileSystemStatistics(this);

    /**
     * Returns a non-{@code null} object which provides statistics about the
     * set of federated file systems managed by this instance.
     * The statistics provided by the returned object get asynchronously
     * updated up to the next call to {@link #sync}. The
     * <p>
     * Note that there may be a slight delay until the values returned reflect
     * the actual state of this package.
     * This delay increases if the system is under heavy load.
     *
     * @see #sync
     * @see ManagedFileSystemStatistics#isClosed
     */
    public synchronized ManagedFileSystemStatistics getStatistics() {
        return statistics;
    }

    @Override
    public synchronized <E extends IOException>
    void sync(  MountPoint prefix,
                ExceptionBuilder<? super IOException, E> builder,
                BitField<SyncOption> options)
    throws E {
        try {
            super.sync(prefix, builder, options);
        } finally {
            statistics.close();
            statistics = new ManagedFileSystemStatistics(this);
        }
    }

    Set<FileSystemController<?>> getControllers() {
        return getControllers(null, null);
    }
}
