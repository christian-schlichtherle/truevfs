/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.inst.jmx;

import de.schlichtherle.truezip.cio.Entry;
import de.schlichtherle.truezip.cio.OutputSocket;
import de.schlichtherle.truezip.fs.inst.InstrumentingOutputSocket;
import java.io.IOException;
import java.io.OutputStream;
import javax.annotation.concurrent.Immutable;

/**
 * @author  Christian Schlichtherle
 */
@Immutable
class JmxOutputSocket<E extends Entry>
extends InstrumentingOutputSocket<E> {
    final JmxIOStatistics stats;

    JmxOutputSocket(OutputSocket<? extends E> model, JmxDirector director, JmxIOStatistics stats) {
        super(model, director);
        assert null != stats;
        this.stats = stats;
    }

    @Override
    public final OutputStream newOutputStream() throws IOException {
        return new JmxOutputStream(getBoundDelegate().newOutputStream(), stats);
    }
}