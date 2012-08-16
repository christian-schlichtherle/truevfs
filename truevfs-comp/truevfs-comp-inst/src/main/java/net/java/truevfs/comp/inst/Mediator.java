/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.inst;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.concurrent.Immutable;
import net.java.truevfs.kernel.spec.FsController;
import net.java.truevfs.kernel.spec.FsManager;
import net.java.truevfs.kernel.spec.FsMetaDriver;
import net.java.truevfs.kernel.spec.FsModel;
import net.java.truevfs.kernel.spec.cio.*;

/**
 * Implements the mediator role of the mediator pattern for instrumenting all
 * objects which are used by the TrueVFS Kernel.
 * When any of the instrumentation methods are called, implementations are
 * given the choice to either return the given object as is or to decorate it
 * with a colleague for instrumentation.
 * <p>
 * Implementations should be thread-safe.
 *
 * @param  <This> the type of this mediator.
 * @author Christian Schlichtherle
 */
@Immutable
public abstract class Mediator<This extends Mediator<This>> {

    /**
     * Instruments the given {@code object}.
     * 
     * @param  object the object to instrument.
     * @return An instrumenting object or {@code object} if the implementation
     *         does not want to instrument it.
     */
    public FsManager instrument(FsManager object) {
        return object;
    }

    /**
     * Instruments the given {@code object}.
     * 
     * @param  object the object to instrument.
     * @return An instrumenting object or {@code object} if the implementation
     *         does not want to instrument it.
     */
    public IoBufferPool instrument(IoBufferPool object) {
        return object;
    }

    /**
     * Instruments the given {@code object}.
     * 
     * @param  origin the instrumenting object which called this method.
     * @param  object the object to instrument.
     * @return An instrumenting object or {@code object} if the implementation
     *         does not want to instrument it.
     * @see    #instrument(FsManager)
     */
    public FsMetaDriver instrument(
            InstrumentingManager<This> origin,
            FsMetaDriver object) {
        return object;
    }

    /**
     * Instruments the given {@code object}.
     * 
     * @param  origin the instrumenting object which called this method.
     * @param  object the object to instrument.
     * @return An instrumenting object or {@code object} if the implementation
     *         does not want to instrument it.
     * @see    #instrument(FsManager)
     */
    public FsController instrument(
            InstrumentingManager<This> origin,
            FsController object) {
        return object;
    }

    /**
     * Instruments the given {@code object}.
     * 
     * @param  origin the instrumenting object which called this method.
     * @param  object the object to instrument.
     * @return An instrumenting object or {@code object} if the implementation
     *         does not want to instrument it.
     * @see    #instrument(IoBufferPool)
     */
    public IoBuffer instrument(
            InstrumentingBufferPool<This> origin,
            IoBuffer object) {
        return object;
    }

    /**
     * Instruments the given {@code object}.
     * 
     * @param  origin the instrumenting object which called this method.
     * @param  object the object to instrument.
     * @return An instrumenting object or {@code object} if the implementation
     *         does not want to instrument it.
     * @see    #instrument(InstrumentingManager, FsMetaDriver)
     */
    public FsModel instrument(
            InstrumentingMetaDriver<This> origin,
            FsModel object) {
        return object;
    }

    /**
     * Instruments the given {@code object}.
     * 
     * @param  origin the instrumenting object which called this method.
     * @param  object the object to instrument.
     * @return An instrumenting object or {@code object} if the implementation
     *         does not want to instrument it.
     * @see    #instrument(InstrumentingManager, FsMetaDriver)
     */
    public FsController instrument(
            InstrumentingMetaDriver<This> origin,
            FsController object) {
        return object;
    }

    /**
     * Instruments the given {@code object}.
     * 
     * @param  <E> the type of the {@linkplain #target() target entry} for I/O
     *         operations.
     * @param  origin the instrumenting object which called this method.
     * @param  object the object to instrument.
     * @return An instrumenting object or {@code object} if the implementation
     *         does not want to instrument it.
     * @see    #instrument(InstrumentingManager, FsController)
     * @see    #instrument(InstrumentingMetaDriver, FsController)
     */
    public <E extends Entry> InputSocket<E> instrument(
            InstrumentingController<This> origin,
            InputSocket<E> object) {
        return object;
    }

    /**
     * Instruments the given {@code object}.
     * 
     * @param  <E> the type of the {@linkplain #target() target entry} for I/O
     *         operations.
     * @param  origin the instrumenting object which called this method.
     * @param  object the object to instrument.
     * @return An instrumenting object or {@code object} if the implementation
     *         does not want to instrument it.
     * @see    #instrument(InstrumentingManager, FsController)
     * @see    #instrument(InstrumentingMetaDriver, FsController)
     */
    public <E extends Entry> OutputSocket<E> instrument(
            InstrumentingController<This> origin,
            OutputSocket<E> object) {
        return object;
    }

    /**
     * Instruments the given {@code object}.
     * 
     * @param  <B> the type of the {@linkplain #target() target entry} for I/O
     *         operations.
     * @param  origin the instrumenting object which called this method.
     * @param  object the object to instrument.
     * @return An instrumenting object or {@code object} if the implementation
     *         does not want to instrument it.
     * @see    #instrument(InstrumentingBufferPool, IoBuffer)
     */
    public <B extends IoBuffer> InputSocket<B> instrument(
            InstrumentingBuffer<This> origin,
            InputSocket<B> object) {
        return object;
    }

    /**
     * Instruments the given {@code object}.
     * 
     * @param  <B> the type of the {@linkplain #target() target entry} for I/O
     *         operations.
     * @param  origin the instrumenting object which called this method.
     * @param  object the object to instrument.
     * @return An instrumenting object or {@code object} if the implementation
     *         does not want to instrument it.
     * @see    #instrument(InstrumentingBufferPool, IoBuffer)
     */
    public <B extends IoBuffer> OutputSocket<B> instrument(
            InstrumentingBuffer<This> origin,
            OutputSocket<B> object) {
        return object;
    }

    /**
     * Instruments the given {@code object}.
     * 
     * @param  <E> the type of the {@linkplain #target() target entry} for I/O
     *         operations.
     * @param  origin the instrumenting object which called this method.
     * @param  object the object to instrument.
     * @return An instrumenting object or {@code object} if the implementation
     *         does not want to instrument it.
     * @see    #instrument(InstrumentingController, InputSocket)
     * @see    #instrument(InstrumentingBuffer, InputSocket)
     */
    public <E extends Entry> InputStream instrument(
            InstrumentingInputSocket<This, E> origin,
            InputStream object) {
        return object;
    }

    /**
     * Instruments the given {@code object}.
     * 
     * @param  <E> the type of the {@linkplain #target() target entry} for I/O
     *         operations.
     * @param  origin the instrumenting object which called this method.
     * @param  object the object to instrument.
     * @return An instrumenting object or {@code object} if the implementation
     *         does not want to instrument it.
     * @see    #instrument(InstrumentingController, InputSocket)
     * @see    #instrument(InstrumentingBuffer, InputSocket)
     */
    public <E extends Entry> SeekableByteChannel instrument(
            InstrumentingInputSocket<This, E> origin,
            SeekableByteChannel object) {
        return object;
    }

    /**
     * Instruments the given {@code object}.
     * 
     * @param  <E> the type of the {@linkplain #target() target entry} for I/O
     *         operations.
     * @param  origin the instrumenting object which called this method.
     * @param  object the object to instrument.
     * @return An instrumenting object or {@code object} if the implementation
     *         does not want to instrument it.
     * @see    #instrument(InstrumentingController, OutputSocket)
     * @see    #instrument(InstrumentingBuffer, OutputSocket)
     */
    public <E extends Entry> OutputStream instrument(
            InstrumentingOutputSocket<This, E> origin,
            OutputStream object) {
        return object;
    }

    /**
     * Instruments the given {@code object}.
     * 
     * @param  <E> the type of the {@linkplain #target() target entry} for I/O
     *         operations.
     * @param  origin the instrumenting object which called this method.
     * @param  object the object to instrument.
     * @return An instrumenting object or {@code object} if the implementation
     *         does not want to instrument it.
     * @see    #instrument(InstrumentingController, OutputSocket)
     * @see    #instrument(InstrumentingBuffer, OutputSocket)
     */
    public <E extends Entry> SeekableByteChannel instrument(
            InstrumentingOutputSocket<This, E> origin,
            SeekableByteChannel object) {
        return object;
    }
}