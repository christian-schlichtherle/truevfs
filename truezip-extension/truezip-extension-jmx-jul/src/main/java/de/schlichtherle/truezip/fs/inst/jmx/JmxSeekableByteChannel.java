/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.inst.jmx;

import de.schlichtherle.truezip.io.DecoratingSeekableByteChannel;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
final class JmxSeekableByteChannel extends DecoratingSeekableByteChannel {
    private final JmxIOStatistics stats;

    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    JmxSeekableByteChannel(@WillCloseWhenClosed SeekableByteChannel sbc, JmxIOStatistics stats) {
        super(sbc);
        assert null != stats;
        this.stats = stats;
    }

    @Override
    public int read(ByteBuffer buf) throws IOException {
        int ret = delegate.read(buf);
        if (0 < ret)
            stats.incBytesRead(ret);
        return ret;
    }

    @Override
    public int write(ByteBuffer buf) throws IOException {
        int ret = delegate.write(buf);
        stats.incBytesWritten(ret);
        return ret;
    }
}
