/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.inst.jmx;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import net.jcip.annotations.ThreadSafe;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@DefaultAnnotation(NonNull.class)
final class JmxIOStatistics {

    private final long time;
    private volatile long read;
    private volatile long written;

    JmxIOStatistics() {
        time = System.currentTimeMillis();
    }

    long getTimeCreatedMillis() {
        return time;
    }
    
    long getBytesRead() {
        return read;
    }

    synchronized void incBytesRead(int inc) {
        read += inc;
    }

    long getBytesWritten() {
        return written;
    }

    synchronized void incBytesWritten(int inc) {
        written += inc;
    }
}
