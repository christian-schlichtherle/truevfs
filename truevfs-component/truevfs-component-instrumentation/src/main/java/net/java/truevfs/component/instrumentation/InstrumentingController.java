/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.component.instrumentation;

import net.java.truevfs.kernel.spec.FsEntryName;
import net.java.truevfs.kernel.spec.FsController;
import net.java.truevfs.kernel.spec.FsAccessOption;
import net.java.truevfs.kernel.spec.FsDecoratingController;
import net.java.truevfs.kernel.spec.FsModel;
import net.java.truecommons.shed.BitField;
import java.util.Objects;
import javax.annotation.concurrent.Immutable;
import net.java.truevfs.kernel.spec.cio.Entry;
import net.java.truevfs.kernel.spec.cio.InputSocket;
import net.java.truevfs.kernel.spec.cio.OutputSocket;

/**
 * @param  <D> the type of the instrumenting director.
 * @author Christian Schlichtherle
 */
@Immutable
public class InstrumentingController<D extends InstrumentingDirector<D>>
extends FsDecoratingController<FsModel, FsController<?>> {

    protected final D director;

    public InstrumentingController(
            final D director,
            final FsController<? extends FsModel> controller) {
        super(controller);
        this.director = Objects.requireNonNull(director);
    }

    @Override
    public InputSocket<? extends Entry> input(BitField<FsAccessOption> options, FsEntryName name) {
        return director.instrument(controller.input(options, name), this);
    }

    @Override
    public OutputSocket<? extends Entry> output(BitField<FsAccessOption> options, FsEntryName name, Entry template) {
        return director.instrument(controller.output(options, name, template), this);
    }
}