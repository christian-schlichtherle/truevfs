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
import java.util.Objects;
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
        this.director = Objects.requireNonNull(director);
    }

    @Override
    public InputSocket<?> input(FsEntryName name, BitField<FsAccessOption> options) {
        return director.instrument(controller.input(name, options), this);
    }

    @Override
    public OutputSocket<?> output(FsEntryName name, BitField<FsAccessOption> options, Entry template) {
        return director.instrument(controller.output(name, options, template), this);
    }
}