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
package de.schlichtherle.truezip.fs.inst.jul;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.fs.FsController;
import de.schlichtherle.truezip.fs.FsModel;
import de.schlichtherle.truezip.fs.inst.InstrumentingCompositeDriver;
import de.schlichtherle.truezip.fs.inst.InstrumentingController;
import de.schlichtherle.truezip.fs.inst.InstrumentingDirector;
import de.schlichtherle.truezip.fs.inst.InstrumentingManager;
import de.schlichtherle.truezip.util.JSE7;
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
public final class JulDirector extends InstrumentingDirector {
    public static final JulDirector SINGLETON = new JulDirector();

    private static final SocketFactory FACTORY = JSE7.AVAILABLE
            ? SocketFactory.NIO
            : SocketFactory.OIO;

    private JulDirector() {
    }

    @Override
    public <E extends IOPool.Entry<E>> IOPool<E> instrument(IOPool<E> pool) {
        return new JulIOPool<E>(pool, this);
    }

    @Override
    public <M extends FsModel> FsController<M> instrument(FsController<M> controller, InstrumentingManager context) {
        return controller;
    }

    @Override
    public <M extends FsModel> FsController<M> instrument(FsController<M> controller, InstrumentingCompositeDriver context) {
        return new InstrumentingController<M>(controller, this);
    }

    @Override
    protected <E extends Entry> InputSocket<E> instrument(InputSocket<E> input) {
        return FACTORY.newInputSocket(input, this);
    }

    @Override
    protected <E extends Entry> OutputSocket<E> instrument(OutputSocket<E> output) {
        return FACTORY.newOutputSocket(output, this);
    }

    private enum SocketFactory {
        OIO() {
            @Override
            <E extends Entry> InputSocket<E> newInputSocket(
                    InputSocket<E> input, JulDirector director) {
                return new JulInputSocket<E>(input, director);
            }

            @Override
            <E extends Entry> OutputSocket<E> newOutputSocket(
                    OutputSocket<E> output, JulDirector director) {
                return new JulOutputSocket<E>(output, director);
            }
        },
        
        NIO() {
            @Override
            <E extends Entry> InputSocket<E> newInputSocket(
                    InputSocket<E> input, JulDirector director) {
                return new JulNio2InputSocket<E>(input, director);
            }

            @Override
            <E extends Entry> OutputSocket<E> newOutputSocket(
                    OutputSocket<E> output, JulDirector director) {
                return new JulNio2OutputSocket<E>(output, director);
            }
        };
        
        abstract <E extends Entry> InputSocket<E> newInputSocket(
                InputSocket<E> input, JulDirector director);

        abstract <E extends Entry> OutputSocket<E> newOutputSocket(
                OutputSocket<E> output, JulDirector director);
    }
}
