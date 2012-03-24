/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.inst.jmx;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.fs.inst.InstrumentingInputSocket;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.entry.InputSocket;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.concurrent.Immutable;

/**
 * @author  Christian Schlichtherle
 */
@Immutable
class JmxInputSocket<E extends Entry>
extends InstrumentingInputSocket<E> {
    final JmxIOStatistics stats;

    JmxInputSocket(InputSocket<? extends E> model, JmxDirector director, JmxIOStatistics stats) {
        super(model, director);
        assert null != stats;
        this.stats = stats;
    }

    @Override
    public final ReadOnlyFile newReadOnlyFile() throws IOException {
        return new JmxReadOnlyFile(getBoundDelegate().newReadOnlyFile(), stats);
    }

    @Override
    public final InputStream newInputStream() throws IOException {
        return new JmxInputStream(getBoundDelegate().newInputStream(), stats);
    }
}