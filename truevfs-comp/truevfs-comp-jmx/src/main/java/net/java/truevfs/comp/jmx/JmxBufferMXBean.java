/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.jmx;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import net.java.truecommons.cio.IoBuffer;

/**
 * An MXBean interface for an {@linkplain IoBuffer I/O buffer}.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
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
