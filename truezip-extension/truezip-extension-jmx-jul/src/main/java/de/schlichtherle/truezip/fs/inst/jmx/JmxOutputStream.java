/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.inst.jmx;

import de.schlichtherle.truezip.io.DecoratingOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import net.jcip.annotations.NotThreadSafe;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
final class JmxOutputStream extends DecoratingOutputStream {
    private final JmxIOStatistics stats;

    JmxOutputStream(OutputStream out, JmxIOStatistics stats) {
        super(out);
        assert null != stats;
        this.stats = stats;
    }

    @Override
    public void write(int b) throws IOException {
        delegate.write(b);
        stats.incBytesWritten(1);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        delegate.write(b, off, len);
        stats.incBytesWritten(len);
    }
    
}
