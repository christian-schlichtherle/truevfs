/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.extension.jmxjul.jmx;

import javax.annotation.Nullable;
import net.truevfs.kernel.cio.IoBuffer;

/**
 * The MXBean interface for an {@link IoBuffer I/O pool entry}.
 *
 * @author  Christian Schlichtherle
 */
public interface JmxIoBufferViewMXBean {
    String getName();
    long getSizeOfData();
    long getSizeOfStorage();
    @Nullable String getTimeWritten();
    @Nullable String getTimeRead();
    @Nullable String getTimeCreated();
}