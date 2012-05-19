/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.extension.jmxjul;

import net.truevfs.kernel.cio.Entry;
import net.truevfs.kernel.cio.InputSocket;
import net.truevfs.kernel.cio.OutputSocket;
import net.truevfs.kernel.FsController;
import net.truevfs.kernel.FsDecoratingController;
import net.truevfs.kernel.FsModel;
import net.truevfs.kernel.FsEntryName;
import net.truevfs.kernel.FsAccessOption;
import net.truevfs.kernel.util.BitField;
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
            final FsController<? extends FsModel> controller,
            final D director) {
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