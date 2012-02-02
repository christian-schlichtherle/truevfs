/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.inst.jmx;

import de.schlichtherle.truezip.socket.IOPool.Entry;
import javax.annotation.Nullable;

/**
 * The MXBean interface for an {@link Entry I/O pool entry}.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public interface JmxIOBufferViewMXBean {
    String getName();
    long getSizeOfData();
    long getSizeOfStorage();
    @Nullable String getTimeWritten();
    @Nullable String getTimeRead();
    @Nullable String getTimeCreated();
}
