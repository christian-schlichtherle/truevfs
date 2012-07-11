/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.extension.pace;

import de.schlichtherle.truezip.fs.FsSyncException;

/**
 * @author Christian Schlichtherle
 */
public interface PaceManagerMXBean {

    /**
     * The name of the property for the maximum number of file systems which
     * may have been mounted at any time, which is {@value}.
     */
    String MAXIMUM_FILE_SYSTEMS_MOUNTED_PROPERTY_NAME
            = "maximumFileSystemsMounted";

    /**
     * The key string for the system property which defines the value of the
     * constant {@link #MAXIMUM_FILE_SYSTEMS_MOUNTED_DEFAULT_VALUE}.
     * Equivalent to the expression
     * {@code PaceManager.class.getName() + "." + MAXIMUM_FILE_SYSTEMS_MOUNTED_PROPERTY_NAME}.
     */
    String MAXIMUM_FILE_SYSTEMS_MOUNTED_PROPERTY_KEY
            = PaceManager.class.getName() + "." + MAXIMUM_FILE_SYSTEMS_MOUNTED_PROPERTY_NAME;

    /**
     * The minimum value for the maximum number of mounted file systems, which
     * is {@value}.
     */
    int MAXIMUM_FILE_SYSTEMS_MOUNTED_MINIMUM_VALUE = 2;

    /**
     * The default value for the maximum number of mounted file systems.
     * The value of this constant will be set to
     * {@link #MAXIMUM_FILE_SYSTEMS_MOUNTED_MINIMUM_VALUE} unless a system
     * property with the key string
     * {@link #MAXIMUM_FILE_SYSTEMS_MOUNTED_PROPERTY_KEY}
     * is set to a value which is greater than
     * {@code MAXIMUM_FILE_SYSTEMS_MOUNTED_MINIMUM_VALUE}.
     * <p>
     * Mind you that this constant is initialized when this interface is loaded
     * and cannot accurately reflect the value in a remote JVM instance.
     */
    int MAXIMUM_FILE_SYSTEMS_MOUNTED_DEFAULT_VALUE
            = Math.max(MAXIMUM_FILE_SYSTEMS_MOUNTED_MINIMUM_VALUE,
                Integer.getInteger(MAXIMUM_FILE_SYSTEMS_MOUNTED_PROPERTY_KEY,
                    MAXIMUM_FILE_SYSTEMS_MOUNTED_MINIMUM_VALUE));

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
     * The value of this property never exceeds
     * {@link #getMaximumFileSystemsMounted()}.
     * <p>
     * Note that you should <em>not</em> use the returned value to synchronize
     * conditionally - this would be unreliable!
     * 
     * @return The number of mounted file systems.
     */
    int getFileSystemsMounted();

    /**
     * Returns the maximum number of file systems which may have been mounted
     * at any time.
     * The mimimum value is {@link #MAXIMUM_FILE_SYSTEMS_MOUNTED_MINIMUM_VALUE}.
     * The default value is {@link #MAXIMUM_FILE_SYSTEMS_MOUNTED_DEFAULT_VALUE}.
     *
     * @return The maximum number of mounted file systems.
     */
    int getMaximumFileSystemsMounted();

    /**
     * Sets the maximum number of file systems which may have been mounted
     * at any time.
     * Changing this property will show effect upon the next access to any
     * file system.
     *
     * @param  maxMounted the maximum number of mounted file systems.
     * @throws IllegalArgumentException if {@code maxMounted} is less than
     *         {@link #MAXIMUM_FILE_SYSTEMS_MOUNTED_MINIMUM_VALUE}.
     */
    void setMaximumFileSystemsMounted(int maxMounted);

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
     * Synchronizes all file systems.
     * As a side effect, upon successful operation, the value of the properties
     * {@link #getFileSystemsMounted()} is reset to zero.
     * 
     * @throws FsSyncException if the synchronization fails for some reason.
     */
    void sync() throws FsSyncException;
}
