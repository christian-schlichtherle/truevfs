/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.extension.jmxjul.jul;

import de.truezip.extension.jmxjul.InstrumentingInputSocket;
import de.truezip.kernel.cio.Entry;
import de.truezip.kernel.cio.InputSocket;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.concurrent.Immutable;

/**
 * @author Christian Schlichtherle
 */
@Immutable
final class JulInputSocket<E extends Entry>
extends InstrumentingInputSocket<E> {

    JulInputSocket(InputSocket<? extends E> model, JulDirector director) {
        super(model, director);
    }

    @Override
    public InputStream stream() throws IOException {
        return new JulInputStream(getBoundSocket());
    }

    @Override
    public java.nio.channels.SeekableByteChannel channel() throws IOException {
        return new JulInputChannel(getBoundSocket());
    }
}
