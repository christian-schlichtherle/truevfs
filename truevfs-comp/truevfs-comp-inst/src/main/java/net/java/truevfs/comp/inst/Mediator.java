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
public interface Mediator<This extends Mediator<This>> {

    /**
     * Instruments the given {@code object}.
     * 
     * @param  object the object to instrument.
     * @return An instrumenting object or {@code object} if the implementation
     *         does not want to instrument it.
     */
    public abstract FsManager instrument(FsManager object);

    /**
     * Instruments the given {@code object}.
     * 
     * @param  object the object to instrument.
     * @return An instrumenting object or {@code object} if the implementation
     *         does not want to instrument it.
     */
    public abstract IoBufferPool instrument(IoBufferPool object);

    /**
     * Instruments the given {@code object}.
     * 
     * @param  origin the instrumenting object which called this method.
     * @param  object the object to instrument.
     * @return An instrumenting object or {@code object} if the implementation
     *         does not want to instrument it.
     * @see    #instrument(FsManager)
     */
    public abstract FsMetaDriver instrument(
            InstrumentingManager<This> origin,
            FsMetaDriver object);

    /**
     * Instruments the given {@code object}.
     * 
     * @param  origin the instrumenting object which called this method.
     * @param  object the object to instrument.
     * @return An instrumenting object or {@code object} if the implementation
     *         does not want to instrument it.
     * @see    #instrument(FsManager)
     */
    public abstract FsController instrument(
            InstrumentingManager<This> origin,
            FsController object);

    /**
     * Instruments the given {@code object}.
     * 
     * @param  origin the instrumenting object which called this method.
     * @param  object the object to instrument.
     * @return An instrumenting object or {@code object} if the implementation
     *         does not want to instrument it.
     * @see    #instrument(IoBufferPool)
     */
    public abstract IoBuffer instrument(
            InstrumentingBufferPool<This> origin,
            IoBuffer object);

    /**
     * Instruments the given {@code object}.
     * 
     * @param  origin the instrumenting object which called this method.
     * @param  object the object to instrument.
     * @return An instrumenting object or {@code object} if the implementation
     *         does not want to instrument it.
     * @see    #instrument(InstrumentingManager, FsMetaDriver)
     */
    public abstract FsModel instrument(
            InstrumentingMetaDriver<This> origin,
            FsModel object);

    /**
     * Instruments the given {@code object}.
     * 
     * @param  origin the instrumenting object which called this method.
     * @param  object the object to instrument.
     * @return An instrumenting object or {@code object} if the implementation
     *         does not want to instrument it.
     * @see    #instrument(InstrumentingManager, FsMetaDriver)
     */
    public abstract FsController instrument(
            InstrumentingMetaDriver<This> origin,
            FsController object);

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
    public abstract <E extends Entry> InputSocket<E> instrument(
            InstrumentingController<This> origin,
            InputSocket<E> object);

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
    public abstract <E extends Entry> OutputSocket<E> instrument(
            InstrumentingController<This> origin,
            OutputSocket<E> object);

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
    public abstract <B extends IoBuffer> InputSocket<B> instrument(
            InstrumentingBuffer<This> origin,
            InputSocket<B> object);

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
    public abstract <B extends IoBuffer> OutputSocket<B> instrument(
            InstrumentingBuffer<This> origin,
            OutputSocket<B> object);

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
    public abstract <E extends Entry> InputStream instrument(
            InstrumentingInputSocket<This, E> origin,
            InputStream object);

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
    public abstract <E extends Entry> SeekableByteChannel instrument(
            InstrumentingInputSocket<This, E> origin,
            SeekableByteChannel object);

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
    public abstract <E extends Entry> OutputStream instrument(
            InstrumentingOutputSocket<This, E> origin,
            OutputStream object);

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
    public abstract <E extends Entry> SeekableByteChannel instrument(
            InstrumentingOutputSocket<This, E> origin,
            SeekableByteChannel object);
}
