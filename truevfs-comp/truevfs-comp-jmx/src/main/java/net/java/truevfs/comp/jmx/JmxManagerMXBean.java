/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.jmx;

import net.java.truevfs.kernel.spec.FsManager;
import net.java.truevfs.kernel.spec.FsSyncException;
import net.java.truevfs.kernel.spec.FsSyncWarningException;

/**
 * The MXBean interface for a {@linkplain FsManager file system manager}.
 *
 * @author Christian Schlichtherle
 */
public interface JmxManagerMXBean {

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
     * The value of this property never exceeds
     * {@link #getFileSystemsTotal()}.
     *
     * @return The total number of <em>top level archive</em> file systems.
     */
    int getTopLevelArchiveFileSystemsTotal();

    /**
     * Returns the number of <em>top level archive</em> file systems
     * which have been mounted and need synchronization by calling
     * {@link #sync}.
     * The value of this property never exceeds
     * {@link #getFileSystemsMounted()}.
     * <p>
     * Note that you should <em>not</em> use the returned value to synchronize
     * conditionally - this would be unreliable!
     *
     * @return The number of mounted <em>top level archive</em> file systems.
     */
    int getTopLevelArchiveFileSystemsMounted();

    /**
     * Synchronizes all managed file systems.
     * As a side effect, upon successful operation, the value of the property
     * {@link #getFileSystemsMounted() fileSystemsMounted} is reset to zero
     * unless some mounted file systems retain elements in their selective
     * entry cache.
     *
     * @throws FsSyncWarningException if <em>only</em> warning conditions
     *         apply.
     *         This implies that the respective parent file system has been
     *         synchronized with constraints, e.g. if an unclosed archive entry
     *         stream gets forcibly closed.
     * @throws FsSyncException if any error conditions apply.
     */
    void sync() throws FsSyncWarningException, FsSyncException;
}
