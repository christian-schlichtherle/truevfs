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
package de.schlichtherle.truezip.io.fs;

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
 * @version $Id$
 */
@ThreadSafe
public final class FSStatisticsManager
extends FSDecoratorManager<FSManager> {

    private volatile FSStatistics statistics
            = new FSStatistics(this);

    /**
     * Constructs a new statistics file system manager.
     *
     * @param manager the decorated file system manager.
     */
    public FSStatisticsManager(@NonNull FSManager manager) {
        super(manager);
    }

    @Override
    public FsController<?> getController(
            final FSMountPoint mountPoint,
            final FSDriver driver) {

        class StatisticsDriver implements FSDriver {
            @Override
            public FsController<?> newController(
                    FSMountPoint mountPoint,
                    FsController<?> parent) {
                final FsController<?> controller
                        = driver.newController(mountPoint, parent);
                return null != parent && null == parent.getParent() // controller is top level federated file system?
                        ? new FSStatisticsController(controller, FSStatisticsManager.this)
                        : controller;
            }
        } // class Driver

        return delegate.getController(mountPoint, new StatisticsDriver());
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
     * @see FSStatistics#isClosed
     */
    @NonNull
    public FSStatistics getStatistics() {
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
    void sync(  BitField<FSSyncOption> options,
                ExceptionHandler<? super IOException, E> handler)
    throws E {
        try {
            super.sync(options, handler);
        } finally {
            statistics = new FSStatistics(this);
        }
    }
}
