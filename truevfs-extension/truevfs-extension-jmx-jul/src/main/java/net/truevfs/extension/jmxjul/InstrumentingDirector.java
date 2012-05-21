/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.extension.jmxjul;

import net.truevfs.kernel.FsCompositeDriver;
import net.truevfs.kernel.FsController;
import net.truevfs.kernel.FsManager;
import net.truevfs.kernel.FsModel;
import net.truevfs.kernel.cio.*;
import javax.annotation.concurrent.Immutable;

/**
 * @param  <D> the type of this instrumenting director.
 * @author Christian Schlichtherle
 */
@Immutable
public abstract class InstrumentingDirector<D extends InstrumentingDirector<D>> {

    public FsManager instrument(FsManager manager) {
        return new InstrumentingManager(manager, this);
    }

    public abstract <B extends IoBuffer<B>> IoPool<B> instrument(IoPool<B> pool);

    protected FsCompositeDriver instrument(
            FsCompositeDriver driver,
            InstrumentingManager context) {
        return new InstrumentingCompositeDriver(driver, this);
    }

    protected FsModel instrument(
            FsModel model,
            InstrumentingCompositeDriver context) {
        return model; //new InstrumentingModel(model, this);
    }

    protected abstract FsController<? extends FsModel> instrument(
            FsController<? extends FsModel> controller,
            InstrumentingManager context);

    protected abstract FsController<? extends FsModel> instrument(
            FsController<? extends FsModel> controller,
            InstrumentingCompositeDriver context);

    protected <B extends IoBuffer<B>> InputSocket<B> instrument(
            InputSocket<B> input,
            InstrumentingIOPool<B>.InstrumentingBuffer context) {
        return instrument(input);
    }

    protected <E extends Entry> InputSocket<E> instrument(
            InputSocket<E> input,
            InstrumentingController<D> context) {
        return instrument(input);
    }

    protected <E extends Entry> InputSocket<E> instrument(
            InputSocket<E> input) {
        return input; //new InstrumentingInputSocket<E>(input, this);
    }

    protected <B extends IoBuffer<B>> OutputSocket<B> instrument(
            OutputSocket<B> output,
            InstrumentingIOPool<B>.InstrumentingBuffer context) {
        return instrument(output);
    }

    protected <E extends Entry> OutputSocket<E> instrument(
            OutputSocket<E> output,
            InstrumentingController<D> context) {
        return instrument(output);
    }

    protected <E extends Entry> OutputSocket<E> instrument(
            OutputSocket<E> output) {
        return output; //new InstrumentingOutputSocket<E>(output, this);
    }
}