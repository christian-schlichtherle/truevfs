/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.component.instrumentation;

import javax.annotation.concurrent.Immutable;
import net.java.truevfs.kernel.spec.FsCompositeDriver;
import net.java.truevfs.kernel.spec.FsController;
import net.java.truevfs.kernel.spec.FsManager;
import net.java.truevfs.kernel.spec.FsModel;
import net.java.truevfs.kernel.spec.cio.*;

/**
 * @param  <This> the type of this instrumenting director.
 * @author Christian Schlichtherle
 */
@Immutable
public abstract class InstrumentingDirector<
        This extends InstrumentingDirector<This>> {

    @SuppressWarnings("unchecked")
    public FsManager instrument(FsManager manager) {
        return new InstrumentingManager<>((This) this, manager);
    }

    @SuppressWarnings("unchecked")
    public <B extends IoBuffer<B>> IoBufferPool<B> instrument(IoBufferPool<B> pool) {
        return new InstrumentingIoBufferPool<>((This) this, pool);
    }

    @SuppressWarnings("unchecked")
    public FsCompositeDriver instrument(
            FsCompositeDriver driver,
            InstrumentingManager<This> context) {
        return new InstrumentingCompositeDriver<>((This) this, driver);
    }

    public FsModel instrument(
            FsModel model,
            InstrumentingCompositeDriver<This> context) {
        return model; //new InstrumentingModel<>((This) this, model);
    }

    public FsController instrument(
            FsController controller,
            InstrumentingManager<This> context) {
        return controller;
    }

    @SuppressWarnings("unchecked")
    public FsController instrument(
            FsController controller,
            InstrumentingCompositeDriver<This> context) {
        return new InstrumentingController<>((This) this, controller);
    }

    public <B extends IoBuffer<B>> InputSocket<B> instrument(
            InputSocket<B> input,
            InstrumentingIoBuffer<This, B> context) {
        return instrument(input);
    }

    public <E extends Entry> InputSocket<E> instrument(
            InputSocket<E> input,
            InstrumentingController<This> context) {
        return instrument(input);
    }

    protected <E extends Entry> InputSocket<E> instrument(
            InputSocket<E> input) {
        return input; //new InstrumentingInputSocket<>((This) this, input);
    }

    public <B extends IoBuffer<B>> OutputSocket<B> instrument(
            OutputSocket<B> output,
            InstrumentingIoBuffer<This, B> context) {
        return instrument(output);
    }

    public <E extends Entry> OutputSocket<E> instrument(
            OutputSocket<E> output,
            InstrumentingController<This> context) {
        return instrument(output);
    }

    protected <E extends Entry> OutputSocket<E> instrument(
            OutputSocket<E> output) {
        return output; //new InstrumentingOutputSocket<>((This) this, output);
    }
}
