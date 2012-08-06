/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.extension.jmx;

import javax.annotation.Nullable;
import net.java.truevfs.kernel.spec.FsModel;
import net.java.truevfs.kernel.spec.FsSyncException;

/**
 * The MXBean interface for a {@link FsModel file system model}.
 *
 * @author Christian Schlichtherle
 */
public interface JmxModelMXBean {
    boolean isMounted();
    String getMountPoint();
    String getMountPointOfParent();
    long getSizeOfData();
    long getSizeOfStorage();
    @Nullable String getTimeWritten();
    @Nullable String getTimeRead();
    @Nullable String getTimeCreated();
    void sync() throws FsSyncException;
}
