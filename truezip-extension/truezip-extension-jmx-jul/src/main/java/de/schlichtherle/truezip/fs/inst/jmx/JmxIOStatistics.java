/*
 * Copyright (C) 2006-2011 Schlichtherle IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
