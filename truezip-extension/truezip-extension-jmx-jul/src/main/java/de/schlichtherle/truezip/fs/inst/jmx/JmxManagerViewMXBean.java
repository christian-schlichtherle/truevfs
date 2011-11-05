/*
 * Copyright 2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.fs.inst.jmx;

import de.schlichtherle.truezip.fs.FsManager;
import de.schlichtherle.truezip.fs.FsSyncException;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * The MXBean interface for a {@link FsManager file system manager}.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
public interface JmxManagerViewMXBean {

    /**
     * Returns a new array of all managed federated file systems.
     * 
     * @return A new array of all managed federated file systems.
     */
    JmxModelViewMXBean[] getFederatedFileSystems();

    /**
     * Returns the total number of managed federated file systems.
     */
    int getFileSystemsTotal();

    /**
     * Returns the number of managed federated file systems which have been
     * touched and need synchronization by calling {@link FsManager#sync}.
     * <p>
     * Note that you should <em>not</em> use the returned value to synchronize
     * conditionally - this is unreliable!
     */
    int getFileSystemsTouched();

    /**
     * Returns the total number of managed <em>top level</em> federated file
     * systems.
     */
    int getTopLevelFileSystemsTotal();

    /**
     * Returns the number of managed <em>top level</em> federated file systems
     * which have been touched and need synchronization by calling
     * {@link FsManager#sync}.
     */
    int getTopLevelFileSystemsTouched();

    /**
     * Unmounts all managed federated file systems.
     * 
     * @throws FsSyncException If any managed federated file system is busy
     *         with I/O.
     */
    void umount() throws FsSyncException;
    
    void clearStatistics();
}
