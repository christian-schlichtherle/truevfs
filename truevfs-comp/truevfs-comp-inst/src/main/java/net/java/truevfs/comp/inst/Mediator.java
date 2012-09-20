/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.inst;

import net.java.truevfs.kernel.spec.FsController;
import net.java.truevfs.kernel.spec.FsManager;
import net.java.truevfs.kernel.spec.FsMetaDriver;
import net.java.truevfs.kernel.spec.FsModel;
import net.java.truevfs.kernel.spec.cio.*;

import javax.annotation.concurrent.Immutable;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;

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
     * Instruments the given {@code subject}.
     * 
     * @param  subject the object to instrument.
     * @return An instrumenting object or {@code subject} if the implementation
     *         does not want to instrument it.
     */
    public FsManager instrument(FsManager subject) {
        return subject;
    }

    /**
     * Instruments the given {@code subject}.
     * 
     * @param  subject the object to instrument.
     * @return An instrumenting object or {@code subject} if the implementation
     *         does not want to instrument it.
     */
    public IoBufferPool instrument(IoBufferPool subject) {
        return subject;
    }

    /**
     * Instruments the given {@code subject}.
     * 
     * @param  origin the instrumenting object which called this method.
     * @param  subject the object to instrument.
     * @return An instrumenting object or {@code subject} if the implementation
     *         does not want to instrument it.
     * @see    #instrument(FsManager)
     */
    public FsMetaDriver instrument(
            InstrumentingManager<This> origin,
            FsMetaDriver subject) {
        return subject;
    }

    /**
     * Instruments the given {@code subject}.
     * 
     * @param  origin the instrumenting object which called this method.
     * @param  subject the object to instrument.
     * @return An instrumenting object or {@code subject} if the implementation
     *         does not want to instrument it.
     * @see    #instrument(FsManager)
     */
    public FsController instrument(
            InstrumentingManager<This> origin,
            FsController subject) {
        return subject;
    }

    /**
     * Instruments the given {@code subject}.
     * 
     * @param  origin the instrumenting object which called this method.
     * @param  subject the object to instrument.
     * @return An instrumenting object or {@code subject} if the implementation
     *         does not want to instrument it.
     * @see    #instrument(IoBufferPool)
     */
    public IoBuffer instrument(
            InstrumentingBufferPool<This> origin,
            IoBuffer subject) {
        return subject;
    }

    /**
     * Instruments the given {@code subject}.
     * 
     * @param  origin the instrumenting object which called this method.
     * @param  subject the object to instrument.
     * @return An instrumenting object or {@code subject} if the implementation
     *         does not want to instrument it.
     * @see    #instrument(InstrumentingManager, FsMetaDriver)
     */
    public FsModel instrument(
            InstrumentingMetaDriver<This> origin,
            FsModel subject) {
        return subject;
    }

    /**
     * Instruments the given {@code subject}.
     * 
     * @param  origin the instrumenting object which called this method.
     * @param  subject the object to instrument.
     * @return An instrumenting object or {@code subject} if the implementation
     *         does not want to instrument it.
     * @see    #instrument(InstrumentingManager, FsMetaDriver)
     */
    public FsController instrument(
            InstrumentingMetaDriver<This> origin,
            FsController subject) {
        return subject;
    }

    /**
     * Instruments the given {@code subject}.
     * 
     * @param  <E> the type of the target entry for I/O operations.
     * @param  origin the instrumenting object which called this method.
     * @param  subject the object to instrument.
     * @return An instrumenting object or {@code subject} if the implementation
     *         does not want to instrument it.
     * @see    #instrument(InstrumentingManager, FsController)
     * @see    #instrument(InstrumentingMetaDriver, FsController)
     */
    public <E extends Entry> InputSocket<E> instrument(
            InstrumentingController<This> origin,
            InputSocket<E> subject) {
        return subject;
    }

    /**
     * Instruments the given {@code subject}.
     *
     * @param  <E> the type of the target entry for I/O operations.
     * @param  origin the instrumenting object which called this method.
     * @param  subject the object to instrument.
     * @return An instrumenting object or {@code subject} if the implementation
     *         does not want to instrument it.
     * @see    #instrument(InstrumentingManager, FsController)
     * @see    #instrument(InstrumentingMetaDriver, FsController)
     */
    public <E extends Entry> OutputSocket<E> instrument(
            InstrumentingController<This> origin,
            OutputSocket<E> subject) {
        return subject;
    }

    /**
     * Instruments the given {@code subject}.
     *
     * @param  <B> the type of the target entry for I/O operations.
     * @param  origin the instrumenting object which called this method.
     * @param  subject the object to instrument.
     * @return An instrumenting object or {@code subject} if the implementation
     *         does not want to instrument it.
     * @see    #instrument(InstrumentingBufferPool, IoBuffer)
     */
    public <B extends IoBuffer> InputSocket<B> instrument(
            InstrumentingBuffer<This> origin,
            InputSocket<B> subject) {
        return subject;
    }

    /**
     * Instruments the given {@code subject}.
     *
     * @param  <B> the type of the target entry for I/O operations.
     * @param  origin the instrumenting object which called this method.
     * @param  subject the object to instrument.
     * @return An instrumenting object or {@code subject} if the implementation
     *         does not want to instrument it.
     * @see    #instrument(InstrumentingBufferPool, IoBuffer)
     */
    public <B extends IoBuffer> OutputSocket<B> instrument(
            InstrumentingBuffer<This> origin,
            OutputSocket<B> subject) {
        return subject;
    }

    /**
     * Instruments the given {@code subject}.
     *
     * @param  <E> the type of the target entry for I/O operations.
     * @param  origin the instrumenting object which called this method.
     * @param  subject the object to instrument.
     * @return An instrumenting object or {@code subject} if the implementation
     *         does not want to instrument it.
     * @see    #instrument(InstrumentingController, InputSocket)
     * @see    #instrument(InstrumentingBuffer, InputSocket)
     */
    public <E extends Entry> InputStream instrument(
            InstrumentingInputSocket<This, E> origin,
            InputStream subject) {
        return subject;
    }

    /**
     * Instruments the given {@code subject}.
     *
     * @param  <E> the type of the target entry for I/O operations.
     * @param  origin the instrumenting object which called this method.
     * @param  subject the object to instrument.
     * @return An instrumenting object or {@code subject} if the implementation
     *         does not want to instrument it.
     * @see    #instrument(InstrumentingController, InputSocket)
     * @see    #instrument(InstrumentingBuffer, InputSocket)
     */
    public <E extends Entry> SeekableByteChannel instrument(
            InstrumentingInputSocket<This, E> origin,
            SeekableByteChannel subject) {
        return subject;
    }

    /**
     * Instruments the given {@code subject}.
     *
     * @param  <E> the type of the target entry for I/O operations.
     * @param  origin the instrumenting object which called this method.
     * @param  subject the object to instrument.
     * @return An instrumenting object or {@code subject} if the implementation
     *         does not want to instrument it.
     * @see    #instrument(InstrumentingController, OutputSocket)
     * @see    #instrument(InstrumentingBuffer, OutputSocket)
     */
    public <E extends Entry> OutputStream instrument(
            InstrumentingOutputSocket<This, E> origin,
            OutputStream subject) {
        return subject;
    }

    /**
     * Instruments the given {@code subject}.
     *
     * @param  <E> the type of the target entry for I/O operations.
     * @param  origin the instrumenting object which called this method.
     * @param  subject the object to instrument.
     * @return An instrumenting object or {@code subject} if the implementation
     *         does not want to instrument it.
     * @see    #instrument(InstrumentingController, OutputSocket)
     * @see    #instrument(InstrumentingBuffer, OutputSocket)
     */
    public <E extends Entry> SeekableByteChannel instrument(
            InstrumentingOutputSocket<This, E> origin,
            SeekableByteChannel subject) {
        return subject;
    }
}
