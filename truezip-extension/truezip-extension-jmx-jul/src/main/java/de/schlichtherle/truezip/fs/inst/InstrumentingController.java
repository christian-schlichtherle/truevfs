/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.inst;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.fs.*;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import javax.annotation.concurrent.Immutable;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
public class InstrumentingController<D extends InstrumentingDirector<D>>
extends FsDecoratingController<FsModel, FsController<?>> {

    protected final D director;

    public InstrumentingController(
            final FsController<?> controller,
            final D director) {
        super(controller);
        if (null == director)
            throw new NullPointerException();
        this.director = director;
    }

    @Override
    public InputSocket<?> getInputSocket(FsEntryName name, BitField<FsInputOption> options) {
        return director.instrument(delegate.getInputSocket(name, options), this);
    }

    @Override
    public OutputSocket<?> getOutputSocket(FsEntryName name, BitField<FsOutputOption> options, Entry template) {
        return director.instrument(delegate.getOutputSocket(name, options, template), this);
    }
}
