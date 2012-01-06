/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
