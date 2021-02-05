/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.commons.jmx;

import global.namespace.truevfs.kernel.api.FsModel;
import global.namespace.truevfs.kernel.api.FsSyncException;

import javax.annotation.Nullable;

/**
 * An MXBean interface for a {@linkplain FsModel file system model}.
 *
 * @author Christian Schlichtherle
 */
public interface JmxModelMXBean {
    boolean          isMounted();
    String           getMountPoint();
    @Nullable String           getMountPointOfParent();
    long             getSizeOfData();
    long             getSizeOfStorage();
    @Nullable String getTimeCreatedDate();
    @Nullable Long   getTimeCreatedMillis();
    @Nullable String getTimeReadDate();
    @Nullable Long   getTimeReadMillis();
    @Nullable String getTimeWrittenDate();
    @Nullable Long   getTimeWrittenMillis();

    void sync() throws FsSyncException;
}
