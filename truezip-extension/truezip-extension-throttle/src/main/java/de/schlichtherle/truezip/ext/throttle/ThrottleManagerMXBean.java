/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.ext.throttle;

import de.schlichtherle.truezip.fs.FsSyncException;

/**
 * @author Christian Schlichtherle
 */
public interface ThrottleManagerMXBean {

    /**
     * The name of the property for the maximum number of archive files which
     * may be mounted at any time, which is {@value}.
     */
    String MAXIMUM_OF_MOST_RECENTLY_USED_ARCHIVE_FILES
            = "maximumOfMostRecentlyUsedArchiveFiles";

    /**
     * The key string for the system property which defines the value of the
     * constant {@link #DEFAULT_MAXIMUM_OF_MOST_RECENTLY_USED_ARCHIVE_FILES}.
     * Equivalent to the expression
     * {@code ThrottleManagerMXBean.class.getName() + "." + MAXIMUM_OF_MOST_RECENTLY_USED_ARCHIVE_FILES}.
     */
    String MAXIMUM_OF_MOST_RECENTLY_USED_ARCHIVE_FILES_PROPERTY_NAME
            = ThrottleManagerMXBean.class.getName() + "." + MAXIMUM_OF_MOST_RECENTLY_USED_ARCHIVE_FILES;

    /**
     * The minimum value for the maximum number of mounted archive files, which
     * is {@value}.
     */
    int MINIMUM_MAXIMUM_OF_MOST_RECENTLY_USED_ARCHIVE_FILES = 2;

    /**
     * The default value for the maximum number of mounted archive files.
     * The value of this constant will be set to
     * {@link #MINIMUM_MAXIMUM_OF_MOST_RECENTLY_USED_ARCHIVE_FILES} unless a system
     * property with the key string
     * {@link #MAXIMUM_OF_MOST_RECENTLY_USED_ARCHIVE_FILES_PROPERTY_NAME}
     * is set to a value which is greater than
     * {@code MINIMUM_MAXIMUM_OF_MOST_RECENTLY_USED_ARCHIVE_FILES}.
     * <p>
     * Mind you that this constant is initialized when this interface is loaded
     * and cannot accurately reflect the value in a remote JVM instance.
     */
    int DEFAULT_MAXIMUM_OF_MOST_RECENTLY_USED_ARCHIVE_FILES
            = Math.max(MINIMUM_MAXIMUM_OF_MOST_RECENTLY_USED_ARCHIVE_FILES,
                Integer.parseInt(System.getProperty(
                    MAXIMUM_OF_MOST_RECENTLY_USED_ARCHIVE_FILES_PROPERTY_NAME,
                    Integer.toString(MINIMUM_MAXIMUM_OF_MOST_RECENTLY_USED_ARCHIVE_FILES))));

    /**
     * Returns the number of managed archive files.
     * 
     * @return The number of managed archive files.
     */
    int getNumberOfManagedArchiveFiles();

    /**
     * Returns the maximum number of archive files which may be mounted at any
     * time.
     * The mimimum value is {@link #MINIMUM_MAXIMUM_OF_MOST_RECENTLY_USED_ARCHIVE_FILES}.
     * The default value is {@link #DEFAULT_MAXIMUM_OF_MOST_RECENTLY_USED_ARCHIVE_FILES}.
     *
     * @return The maximum number of archive files which may be mounted at any
     *         time.
     */
    int getMaximumOfMostRecentlyUsedArchiveFiles();

    /**
     * Sets the maximum number of archive files which may be mounted at any
     * time.
     * Changing this property will show effect upon the next access to an
     * archive file.
     *
     * @param  maxMounts the maximum number of mounted archive files.
     * @throws IllegalArgumentException if {@code maxMounts} is less than
     *         {@link #MINIMUM_MAXIMUM_OF_MOST_RECENTLY_USED_ARCHIVE_FILES}.
     */
    void setMaximumOfMostRecentlyUsedArchiveFiles(int maxMounts);

    /**
     * Returns the number of most recently used archive files.
     * The value of this property never exceeds
     * {@link #getMaximumOfMostRecentlyUsedArchiveFiles()} nor
     * {@link #getNumberOfManagedArchiveFiles()}.
     * 
     * @return The number of most recently used archive files.
     */
    int getNumberOfMostRecentlyUsedArchiveFiles();

    /**
     * Returns the number of archive files which have been evicted from the
     * cache of most recently used archive files but not yet synced.
     * <p>
     * The value of this property should be zero unless the following applies:
     * <ul>
     * <li>There is lots of archive file activity caused by different threads.
     *     In this case the number should decrease to zero as soon as possible.
     * <li>Some evicted archive files are direct or indirect parents of the
     *     most recently used archive files.
     *     In that case the number will stay until the direct or indirect
     *     children get evicted from the cache, too.
     * </ul>
     * In either case, a successful {@link #sync()} will reset the value of
     * this property to zero.
     * 
     * @return The number of archive files which have been evicted from the
     *         cache of most recently used archive files but not yet synced.
     */
    int getNumberOfLeastRecentlyUsedArchiveFiles();

    /**
     * Syncs all managed archive files.
     * As a side effect, upon successful operation, the value of the properties
     * {@link #getNumberOfMostRecentlyUsedArchiveFiles()} and
     * {@link #getNumberOfLeastRecentlyUsedArchiveFiles()}
     * is reset to zero.
     * 
     * @throws FsSyncException if the synchronization fails for some reason.
     */
    void sync() throws FsSyncException;
}
