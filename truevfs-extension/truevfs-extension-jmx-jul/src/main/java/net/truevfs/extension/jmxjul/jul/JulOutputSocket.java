/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.extension.jmxjul.jul;

import net.truevfs.extension.jmxjul.InstrumentingOutputSocket;
import net.truevfs.kernel.cio.Entry;
import net.truevfs.kernel.cio.OutputSocket;
import java.io.IOException;
import java.io.OutputStream;
import javax.annotation.concurrent.Immutable;

/**
 * @author  Christian Schlichtherle
 */
@Immutable
final class JulOutputSocket<E extends Entry>
extends InstrumentingOutputSocket<E> {

    JulOutputSocket(OutputSocket<? extends E> model, JulDirector director) {
        super(model, director);
    }

    @Override
    public OutputStream stream() throws IOException {
        return new JulOutputStream(boundSocket());
    }
}
