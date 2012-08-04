/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.component.instrumentation;

import java.util.Objects;
import javax.annotation.concurrent.Immutable;
import net.java.truecommons.shed.BitField;
import net.java.truevfs.kernel.spec.*;
import net.java.truevfs.kernel.spec.cio.Entry;
import net.java.truevfs.kernel.spec.cio.InputSocket;
import net.java.truevfs.kernel.spec.cio.OutputSocket;

/**
 * @param  <D> the type of the instrumenting director.
 * @param  <M> the type of the instrumented file system model.
 * @author Christian Schlichtherle
 */
@Immutable
public class InstrumentingController<D extends InstrumentingDirector<D>>
extends FsDecoratingController<FsController> {
    protected final D director;

    public InstrumentingController(
            final D director,
            final FsController controller) {
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
