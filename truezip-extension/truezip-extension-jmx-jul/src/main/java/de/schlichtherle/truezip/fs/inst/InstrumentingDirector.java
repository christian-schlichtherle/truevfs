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
import de.schlichtherle.truezip.fs.FsCompositeDriver;
import de.schlichtherle.truezip.fs.FsController;
import de.schlichtherle.truezip.fs.FsManager;
import de.schlichtherle.truezip.fs.FsModel;
import de.schlichtherle.truezip.socket.IOPool;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.socket.OutputSocket;
import net.jcip.annotations.Immutable;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
public abstract class InstrumentingDirector<D extends InstrumentingDirector<D>> {

    public abstract <E extends IOPool.Entry<E>> IOPool<E> instrument(IOPool<E> pool);

    public FsManager instrument(FsManager manager) {
        return new InstrumentingManager(manager, this);
    }

    public FsCompositeDriver instrument(FsCompositeDriver driver, InstrumentingManager context) {
        return new InstrumentingCompositeDriver(driver, this);
    }

    public FsModel instrument(FsModel model, InstrumentingCompositeDriver context) {
        return model; //new InstrumentingModel(model, this);
    }

    public abstract FsController<?> instrument(FsController<?> controller, InstrumentingManager context);

    public abstract FsController<?> instrument(FsController<?> controller, InstrumentingCompositeDriver context);

    public <E extends IOPool.Entry<E>> InputSocket<E> instrument(InputSocket<E> input, InstrumentingIOPool<E, D>.IOBuffer context) {
        return instrument(input);
    }

    public <E extends Entry> InputSocket<E> instrument(InputSocket<E> input, InstrumentingController<D> context) {
        return instrument(input);
    }

    protected <E extends Entry> InputSocket<E> instrument(InputSocket<E> input) {
        return input; //new InstrumentingInputSocket<E>(input, this);
    }

    public <E extends IOPool.Entry<E>> OutputSocket<E> instrument(OutputSocket<E> output, InstrumentingIOPool<E, D>.IOBuffer context) {
        return instrument(output);
    }

    public <E extends Entry> OutputSocket<E> instrument(OutputSocket<E> output, InstrumentingController<D> context) {
        return instrument(output);
    }

    protected <E extends Entry> OutputSocket<E> instrument(OutputSocket<E> output) {
        return output; //new InstrumentingOutputSocket<E>(output, this);
    }
}
