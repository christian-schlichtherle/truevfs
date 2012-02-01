/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.inst.jul;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.fs.inst.InstrumentingInputSocket;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.socket.InputSocket;
import java.io.IOException;
import java.io.InputStream;
import net.jcip.annotations.Immutable;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
class JulInputSocket<E extends Entry>
extends InstrumentingInputSocket<E> {

    JulInputSocket(InputSocket<? extends E> model, JulDirector director) {
        super(model, director);
    }

    @Override
    public final ReadOnlyFile newReadOnlyFile() throws IOException {
        return new JulReadOnlyFile<E>(getBoundSocket());
    }

    @Override
    public final InputStream newInputStream() throws IOException {
        return new JulInputStream<E>(getBoundSocket());
    }
}
