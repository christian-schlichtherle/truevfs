/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.inst.jul;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.fs.inst.InstrumentingOutputSocket;
import de.schlichtherle.truezip.socket.OutputSocket;
import java.io.IOException;
import java.io.OutputStream;
import javax.annotation.concurrent.Immutable;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
class JulOutputSocket<E extends Entry>
extends InstrumentingOutputSocket<E> {

    JulOutputSocket(OutputSocket<? extends E> model, JulDirector director) {
        super(model, director);
    }

    @Override
    public final OutputStream newOutputStream() throws IOException {
        return new JulOutputStream<E>(getBoundSocket());
    }
}
