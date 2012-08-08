/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx;

import javax.annotation.concurrent.ThreadSafe;
import net.java.truevfs.kernel.spec.FsManager;

/**
 * The MXBean interface for a {@linkplain FsManager file system manager}.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
public interface JmxManagerMXBean
extends net.java.truevfs.comp.jmx.JmxManagerMXBean {
    void clearStatistics();
}
