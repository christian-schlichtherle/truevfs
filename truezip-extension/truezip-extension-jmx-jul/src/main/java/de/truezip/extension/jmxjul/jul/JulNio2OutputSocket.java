/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.extension.jmxjul.jul;

import de.truezip.kernel.cio.Entry;
import de.truezip.kernel.cio.OutputSocket;
import java.io.IOException;
import javax.annotation.concurrent.Immutable;

/**
 * @author  Christian Schlichtherle
 */
@Immutable
final class JulNio2OutputSocket<E extends Entry>
extends JulOutputSocket<E> {

    JulNio2OutputSocket(OutputSocket<? extends E> model, JulDirector director) {
        super(model, director);
    }

    @Override
    public java.nio.channels.SeekableByteChannel newSeekableByteChannel() throws IOException {
        final java.nio.channels.SeekableByteChannel sbc = getBoundDelegate().newSeekableByteChannel();
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