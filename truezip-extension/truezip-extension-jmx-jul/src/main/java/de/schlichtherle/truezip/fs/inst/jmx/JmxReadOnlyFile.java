/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.inst.jmx;

import de.schlichtherle.truezip.rof.DecoratingReadOnlyFile;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import java.io.IOException;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
final class JmxReadOnlyFile extends DecoratingReadOnlyFile {
    private final JmxIOStatistics stats;

    JmxReadOnlyFile(ReadOnlyFile rof, JmxIOStatistics stats) {
        super(rof);
        assert null != stats;
        this.stats = stats;
    }

    @Override
    public int read() throws IOException {
        int ret = delegate.read();
        if (0 < ret)
            stats.incBytesRead(1);
        return ret;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int ret = delegate.read(b, off, len);
        if (0 < ret)
            stats.incBytesRead(ret);
        return ret;
    }
}
