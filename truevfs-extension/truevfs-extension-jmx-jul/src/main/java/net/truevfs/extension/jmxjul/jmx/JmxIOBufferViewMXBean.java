/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.extension.jmxjul.jmx;

import net.truevfs.kernel.cio.IOBuffer;
import javax.annotation.Nullable;

/**
 * The MXBean interface for an {@link IOBuffer I/O pool entry}.
 *
 * @author  Christian Schlichtherle
 */
public interface JmxIOBufferViewMXBean {
    String getName();
    long getSizeOfData();
    long getSizeOfStorage();
    @Nullable String getTimeWritten();
    @Nullable String getTimeRead();
    @Nullable String getTimeCreated();
}