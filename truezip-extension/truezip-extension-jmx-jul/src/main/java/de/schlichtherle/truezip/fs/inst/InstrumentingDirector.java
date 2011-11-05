/*
 * Copyright 2011 Schlichtherle IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import net.jcip.annotations.Immutable;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@DefaultAnnotation(NonNull.class)
public abstract class InstrumentingDirector {

    /**
     * Call this method to make a convenient null pointer check.
     *
     * @return this;
     */
    public final InstrumentingDirector check() {
        return this;
    }

    public <E extends IOPool.Entry<E>> IOPool<E> instrument(IOPool<E> pool) {
        return new InstrumentingIOPool<E>(pool, this);
    }

    public FsManager instrument(FsManager manager) {
        return new InstrumentingManager(manager, this);
    }

    public FsCompositeDriver instrument(FsCompositeDriver driver, InstrumentingManager context) {
        return new InstrumentingCompositeDriver(driver, this);
    }

    public FsModel instrument(FsModel model, InstrumentingCompositeDriver context) {
        return model; //new InstrumentingModel(model, this);
    }

    public abstract <M extends FsModel> FsController<M> instrument(FsController<M> controller, InstrumentingManager context);

    public abstract <M extends FsModel> FsController<M> instrument(FsController<M> controller, InstrumentingCompositeDriver context);

    public <E extends IOPool.Entry<E>> InputSocket<E> instrument(InputSocket<E> input, InstrumentingIOPool<E>.InstrumentingEntry context) {
        return instrument(input);
    }

    public <E extends Entry, M extends FsModel> InputSocket<E> instrument(InputSocket<E> input, InstrumentingController<M> context) {
        return instrument(input);
    }

    protected <E extends Entry> InputSocket<E> instrument(InputSocket<E> input) {
        return input; //new InstrumentingInputSocket<E>(input, this);
    }

    public <E extends IOPool.Entry<E>> OutputSocket<E> instrument(OutputSocket<E> output, InstrumentingIOPool<E>.InstrumentingEntry context) {
        return instrument(output);
    }

    public <E extends Entry, M extends FsModel> OutputSocket<E> instrument(OutputSocket<E> output, InstrumentingController<M> context) {
        return instrument(output);
    }

    protected <E extends Entry> OutputSocket<E> instrument(OutputSocket<E> output) {
        return output; //new InstrumentingOutputSocket<E>(output, this);
    }
}
