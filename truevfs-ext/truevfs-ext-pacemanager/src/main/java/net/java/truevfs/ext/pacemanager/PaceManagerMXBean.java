/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.pacemanager;

import net.java.truevfs.comp.jmx.JmxManagerMXBean;

/**
 * An MXBean interface for a {@linkplain PaceManager pace manager}.
 * 
 * @author Christian Schlichtherle
 */
public interface PaceManagerMXBean extends JmxManagerMXBean {

    /**
     * The key string for the system property which defines the value of the
     * constant {@link #MAXIMUM_FILE_SYSTEMS_MOUNTED_DEFAULT_VALUE},
     * which is equivalent to the expression
     * {@code PaceManagerMXBean.class.getPackage().getName() + ".maximumFileSystemsMounted"}.
     */
    String MAXIMUM_FILE_SYSTEMS_MOUNTED_PROPERTY_KEY =
            PaceManagerMXBean.class.getPackage().getName() +
            ".maximumFileSystemsMounted";

    /**
     * The minimum value for the maximum number of mounted file systems,
     * which is {@value}.
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
}
