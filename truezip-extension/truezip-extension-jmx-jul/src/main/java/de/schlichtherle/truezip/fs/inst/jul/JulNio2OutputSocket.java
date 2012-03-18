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
import de.schlichtherle.truezip.socket.OutputSocket;
import java.io.IOException;
import javax.annotation.concurrent.Immutable;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
final class JulNio2OutputSocket<E extends Entry>
extends JulOutputSocket<E> {

    JulNio2OutputSocket(OutputSocket<? extends E> model, JulDirector director) {
        super(model, director);
    }

    @Override
    public java.nio.channels.SeekableByteChannel newSeekableByteChannel() throws IOException {
        final java.nio.channels.SeekableByteChannel sbc = getBoundSocket().newSeekableByteChannel();
        try {
            return new JulOutputByteChannel<E>(sbc, this);
        } catch (IOException ex) {
            try {
                sbc.close();
            } catch (IOException ex2) {
                throw (IOException) ex2.initCause(ex);
            }
            throw ex;
        }
    }
}
