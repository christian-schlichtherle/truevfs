/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.extension.jmxjul.jmx;

import de.truezip.kernel.rof.DecoratingReadOnlyFile;
import de.truezip.kernel.rof.ReadOnlyFile;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * @author  Christian Schlichtherle
 */
@NotThreadSafe
final class JmxReadOnlyFile extends DecoratingReadOnlyFile {
    private final JmxIOStatistics stats;

    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    JmxReadOnlyFile(@WillCloseWhenClosed ReadOnlyFile rof, JmxIOStatistics stats) {
        super(rof);
        assert null != stats;
        this.stats = stats;
    }

    @Override
    public int read() throws IOException {
        int ret = rof.read();
        if (0 < ret)
            stats.incBytesRead(1);
        return ret;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int ret = rof.read(b, off, len);
        if (0 < ret)
            stats.incBytesRead(ret);
        return ret;
    }
}