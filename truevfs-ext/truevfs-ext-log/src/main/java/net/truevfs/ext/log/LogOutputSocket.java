/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.ext.log;

import java.io.IOException;
import java.io.OutputStream;
import javax.annotation.concurrent.Immutable;
import net.truevfs.comp.inst.InstrumentingOutputSocket;
import net.truevfs.kernel.spec.cio.Entry;
import net.truevfs.kernel.spec.cio.InputSocket;
import net.truevfs.kernel.spec.cio.OutputSocket;

/**
 * @author  Christian Schlichtherle
 */
@Immutable
final class LogOutputSocket<E extends Entry>
extends InstrumentingOutputSocket<E> {

    LogOutputSocket(LogDirector director, OutputSocket<? extends E> model) {
        super(director, model);
    }

    @Override
    public OutputStream stream(InputSocket<? extends Entry> peer)
    throws IOException {
        return new LogOutputStream(socket(), peer);
    }
}
