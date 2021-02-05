/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.commons.jmx;

import global.namespace.truevfs.commons.cio.IoBuffer;

import javax.annotation.Nullable;

/**
 * An MXBean interface for an {@linkplain IoBuffer I/O buffer}.
 *
 * @author Christian Schlichtherle
 */
public interface JmxBufferMXBean {
    String           getName();
    long             getSizeOfData();
    long             getSizeOfStorage();
    @Nullable String getTimeCreatedDate();
    @Nullable Long   getTimeCreatedMillis();
    @Nullable String getTimeReadDate();
    @Nullable Long   getTimeReadMillis();
    @Nullable String getTimeWrittenDate();
    @Nullable Long   getTimeWrittenMillis();
}
