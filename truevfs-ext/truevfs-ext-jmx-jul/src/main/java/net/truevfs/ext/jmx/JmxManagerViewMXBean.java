/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.ext.jmx;

import net.truevfs.kernel.spec.FsManager;
import net.truevfs.kernel.spec.FsSyncException;

/**
 * The MXBean interface for a {@link FsManager file system manager}.
 *
 * @author Christian Schlichtherle
 */
public interface JmxManagerViewMXBean {

    /**
     * Returns the total number of file systems.
     * 
     * @return The total number of file systems.
     */
    int getFileSystemsTotal();

    /**
     * Returns the number of file systems
     * which have been mounted and need synchronization by calling
     * {@link #sync}.
     * <p>
     * Note that you should <em>not</em> use the returned value to synchronize
     * conditionally - this would be unreliable!
     * 
     * @return The number of mounted file systems.
     */
    int getFileSystemsMounted();

    /**
     * Returns the total number of <em>top level archive</em> file systems.
     * 
     * @return The total number of <em>top level archive</em> file systems.
     */
    int getTopLevelArchiveFileSystemsTotal();

    /**
     * Returns the number of <em>top level archive</em> file systems
     * which have been mounted and need synchronization by calling
     * {@link #sync}.
     * <p>
     * Note that you should <em>not</em> use the returned value to synchronize
     * conditionally - this would be unreliable!
     * 
     * @return The number of mounted <em>top level archive</em> file systems.
     */
    int getTopLevelArchiveFileSystemsMounted();

    /**
     * Synchronizes all managed file systems.
     * 
     * @throws FsSyncException If any managed file system is busy
     *         with I/O.
     */
    void sync() throws FsSyncException;
    
    void clearStatistics();
}