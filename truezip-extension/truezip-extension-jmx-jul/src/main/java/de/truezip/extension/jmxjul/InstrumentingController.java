/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.extension.jmxjul;

import de.truezip.kernel.cio.Entry;
import de.truezip.kernel.cio.InputSocket;
import de.truezip.kernel.cio.OutputSocket;
import de.truezip.kernel.FsController;
import de.truezip.kernel.FsDecoratingController;
import de.truezip.kernel.FsModel;
import de.truezip.kernel.FsEntryName;
import de.truezip.kernel.FsAccessOption;
import de.truezip.kernel.util.BitField;
import javax.annotation.concurrent.Immutable;

/**
 * @param  <D> the type of the instrumenting director.
 * @author Christian Schlichtherle
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
    public InputSocket<?> getInputSocket(FsEntryName name, BitField<FsAccessOption> options) {
        return director.instrument(controller.getInputSocket(name, options), this);
    }

    @Override
    public OutputSocket<?> getOutputSocket(FsEntryName name, BitField<FsAccessOption> options, Entry template) {
        return director.instrument(controller.getOutputSocket(name, options, template), this);
    }
}