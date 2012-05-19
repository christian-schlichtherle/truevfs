/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.extension.jmxjul.jul;

import net.truevfs.extension.jmxjul.InstrumentingInputSocket;
import net.truevfs.kernel.cio.Entry;
import net.truevfs.kernel.cio.InputSocket;
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
        return new JulInputStream(boundSocket());
    }

    @Override
    public java.nio.channels.SeekableByteChannel channel() throws IOException {
        return new JulInputChannel(boundSocket());
    }
}
