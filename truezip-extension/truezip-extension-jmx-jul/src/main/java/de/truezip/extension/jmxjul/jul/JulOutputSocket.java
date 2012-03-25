/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.extension.jmxjul.jul;

import de.schlichtherle.truezip.cio.Entry;
import de.truezip.extension.jmxjul.InstrumentingOutputSocket;
import de.schlichtherle.truezip.cio.OutputSocket;
import java.io.IOException;
import java.io.OutputStream;
import javax.annotation.concurrent.Immutable;

/**
 * @author  Christian Schlichtherle
 */
@Immutable
class JulOutputSocket<E extends Entry>
extends InstrumentingOutputSocket<E> {

    JulOutputSocket(OutputSocket<? extends E> model, JulDirector director) {
        super(model, director);
    }

    @Override
    public final OutputStream newOutputStream() throws IOException {
        return new JulOutputStream<E>(getBoundDelegate());
    }
}