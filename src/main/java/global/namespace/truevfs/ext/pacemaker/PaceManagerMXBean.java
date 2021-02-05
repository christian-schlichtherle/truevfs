/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.ext.pacemaker;

import global.namespace.truevfs.commons.jmx.JmxManagerMXBean;

/**
 * The MXBean interface for a {@link PaceManager}.
 *
 * @author Christian Schlichtherle
 */
public interface PaceManagerMXBean extends JmxManagerMXBean {

    /**
     * Returns the maximum number of file systems which may have been mounted
     * at any time.
     * The minimum value is `MAXIMUM_FILE_SYSTEMS_MOUNTED_MINIMUM_VALUE`.
     * The default value is `MAXIMUM_FILE_SYSTEMS_MOUNTED_DEFAULT_VALUE`.
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
     * @param maxMounted the maximum number of mounted file systems.
     * @throws IllegalArgumentException if `maxMounted` is less than
     *                                  `MAXIMUM_FILE_SYSTEMS_MOUNTED_MINIMUM_VALUE`.
     */
    void setMaximumFileSystemsMounted(int maxMounted);
}
