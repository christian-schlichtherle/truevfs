/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.inst.jmx;

import net.jcip.annotations.ThreadSafe;

/**
 * Provides statistics for the federated file systems managed by a file system
 * manager.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
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
