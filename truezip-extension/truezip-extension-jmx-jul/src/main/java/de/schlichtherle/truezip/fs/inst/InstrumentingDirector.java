/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.inst;

import de.schlichtherle.truezip.entry.*;
import de.schlichtherle.truezip.fs.FsCompositeDriver;
import de.schlichtherle.truezip.fs.FsController;
import de.schlichtherle.truezip.fs.FsManager;
import de.schlichtherle.truezip.fs.FsModel;
import javax.annotation.concurrent.Immutable;

/**
 * @param  <D> the type of this instrumenting director.
 * @author Christian Schlichtherle
 */
@Immutable
public abstract class InstrumentingDirector<D extends InstrumentingDirector<D>> {

    public abstract <B extends IOBuffer<B>> IOPool<B> instrument(IOPool<B> pool);

    public FsManager instrument(FsManager manager) {
        return new InstrumentingManager(manager, this);
    }

    public FsCompositeDriver instrument(
            FsCompositeDriver driver,
            InstrumentingManager context) {
        return new InstrumentingCompositeDriver(driver, this);
    }

    public FsModel instrument(
            FsModel model,
            InstrumentingCompositeDriver context) {
        return model; //new InstrumentingModel(model, this);
    }

    public abstract FsController<?> instrument(
            FsController<?> controller,
            InstrumentingManager context);

    public abstract FsController<?> instrument(
            FsController<?> controller,
            InstrumentingCompositeDriver context);

    public <B extends IOBuffer<B>> InputSocket<B> instrument(
            InputSocket<B> input,
            InstrumentingIOPool<B>.InstrumentingBuffer context) {
        return instrument(input);
    }

    public <E extends Entry> InputSocket<E> instrument(
            InputSocket<E> input,
            InstrumentingController<D> context) {
        return instrument(input);
    }

    protected <E extends Entry> InputSocket<E> instrument(
            InputSocket<E> input) {
        return input; //new InstrumentingInputSocket<E>(input, this);
    }

    public <B extends IOBuffer<B>> OutputSocket<B> instrument(
            OutputSocket<B> output,
            InstrumentingIOPool<B>.InstrumentingBuffer context) {
        return instrument(output);
    }

    public <E extends Entry> OutputSocket<E> instrument(
            OutputSocket<E> output,
            InstrumentingController<D> context) {
        return instrument(output);
    }

    protected <E extends Entry> OutputSocket<E> instrument(
            OutputSocket<E> output) {
        return output; //new InstrumentingOutputSocket<E>(output, this);
    }
}