/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.extension.jmxjul.jmx;

import net.truevfs.kernel.FsModel;
import net.truevfs.kernel.FsSyncException;
import javax.annotation.Nullable;

/**
 * The MXBean interface for a {@link FsModel file system model}.
 *
 * @author  Christian Schlichtherle
 */
public interface JmxModelViewMXBean {
    String getMountPoint();
    boolean isTouched();
    @Nullable JmxModelViewMXBean getModelOfParent();
    String getMountPointOfParent();
    long getSizeOfData();
    long getSizeOfStorage();
    @Nullable String getTimeWritten();
    @Nullable String getTimeRead();
    @Nullable String getTimeCreated();
    void umount() throws FsSyncException;
}