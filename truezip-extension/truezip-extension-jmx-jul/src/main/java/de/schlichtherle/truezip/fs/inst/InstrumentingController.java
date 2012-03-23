/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.inst;

import de.schlichtherle.truezip.fs.addr.FsEntryName;
import de.schlichtherle.truezip.fs.option.FsOutputOption;
import de.schlichtherle.truezip.fs.option.FsInputOption;
import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.fs.*;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import javax.annotation.concurrent.Immutable;

/**
 * @author  Christian Schlichtherle
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