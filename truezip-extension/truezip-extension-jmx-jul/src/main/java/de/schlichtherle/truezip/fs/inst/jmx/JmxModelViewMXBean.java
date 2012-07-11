/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.inst.jmx;

import de.schlichtherle.truezip.fs.FsModel;
import de.schlichtherle.truezip.fs.FsSyncException;
import javax.annotation.Nullable;

/**
 * The MXBean interface for a {@link FsModel file system model}.
 *
 * @author Christian Schlichtherle
 */
public interface JmxModelViewMXBean {
    String getMountPoint();
    boolean isMounted();
    String getParentMountPoint();
    long getSizeOfData();
    long getSizeOfStorage();
    @Nullable String getTimeWritten();
    @Nullable String getTimeRead();
    @Nullable String getTimeCreated();
    void sync() throws FsSyncException;
}
