/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.inst.jul;

import de.schlichtherle.truezip.cio.Entry;
import de.schlichtherle.truezip.fs.inst.InstrumentingInputSocket;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.cio.InputSocket;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.concurrent.Immutable;

/**
 * @author  Christian Schlichtherle
 */
@Immutable
class JulInputSocket<E extends Entry>
extends InstrumentingInputSocket<E> {

    JulInputSocket(InputSocket<? extends E> model, JulDirector director) {
        super(model, director);
    }

    @Override
    public final ReadOnlyFile newReadOnlyFile() throws IOException {
        return new JulReadOnlyFile<E>(getBoundDelegate());
    }

    @Override
    public final InputStream newInputStream() throws IOException {
        return new JulInputStream<E>(getBoundDelegate());
    }
}