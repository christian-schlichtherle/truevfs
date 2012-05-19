/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.extension.jmxjul.jmx;

import javax.annotation.concurrent.ThreadSafe;

/**
 * Provides statistics for the federated file systems managed by a file system
 * manager.
 *
 * @author  Christian Schlichtherle
 */
@ThreadSafe
public interface JmxIOStatisticsMXBean {

    String getType();

    String getTimeCreated();

    long getTimeCreatedMillis();

    long getBytesRead();

    long getBytesWritten();

    void close();
}