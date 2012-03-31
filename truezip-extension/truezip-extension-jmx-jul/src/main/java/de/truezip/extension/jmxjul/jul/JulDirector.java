/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.extension.jmxjul.jul;

import de.truezip.extension.jmxjul.InstrumentingCompositeDriver;
import de.truezip.extension.jmxjul.InstrumentingController;
import de.truezip.extension.jmxjul.InstrumentingDirector;
import de.truezip.extension.jmxjul.InstrumentingManager;
import de.truezip.kernel.cio.*;
import de.truezip.kernel.FsController;
import de.truezip.kernel.util.JSE7;
import javax.annotation.concurrent.Immutable;

/**
 * @author Christian Schlichtherle
 */
@Immutable
public final class JulDirector extends InstrumentingDirector<JulDirector> {
    public static final JulDirector SINGLETON = new JulDirector();

    private static final SocketFactory FACTORY = JSE7.AVAILABLE
            ? SocketFactory.NIO
            : SocketFactory.OIO;

    private JulDirector() { }

    @Override
    public <B extends IOBuffer<B>> IOPool<B> instrument(IOPool<B> pool) {
        return new JulIOPool<B>(pool, this);
    }

    @Override
    protected FsController<?> instrument(FsController<?> controller, InstrumentingManager context) {
        return controller;
    }

    @Override
    protected FsController<?> instrument(FsController<?> controller, InstrumentingCompositeDriver context) {
        return new InstrumentingController<JulDirector>(controller, this);
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
        },
        
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
        };
        
        abstract <E extends Entry> InputSocket<E> newInputSocket(
                InputSocket<E> input, JulDirector director);

        abstract <E extends Entry> OutputSocket<E> newOutputSocket(
                OutputSocket<E> output, JulDirector director);
    }
}
