/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.inst;

import global.namespace.truevfs.comp.cio.*;
import global.namespace.truevfs.kernel.api.FsCompositeDriver;
import global.namespace.truevfs.kernel.api.FsController;
import global.namespace.truevfs.kernel.api.FsManager;
import global.namespace.truevfs.kernel.api.FsModel;

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
 * Subclasses should be thread-safe.
 *
 * @param  <This> the type of this mediator.
 * @author Christian Schlichtherle
 */
public abstract class Mediator<This extends Mediator<This>> {

    /**
     * Instruments the given {@code subject}.
     *
     * @param  subject the subject of instrumentation.
     * @return An instrumenting object or {@code subject} if the implementation
     *         does not want to instrument it.
     */
    public FsManager instrument(FsManager subject) {
        return subject;
    }

    /**
     * Instruments the given {@code subject}.
     *
     * @param  subject the subject of instrumentation.
     * @return An instrumenting object or {@code subject} if the implementation
     *         does not want to instrument it.
     */
    public IoBufferPool instrument(IoBufferPool subject) {
        return subject;
    }

    /**
     * Instruments the given {@code subject}.
     *
     * @param  context the instrumenting object which called this method.
     * @param  subject the subject of instrumentation.
     * @return An instrumenting object or {@code subject} if the implementation
     *         does not want to instrument it.
     * @see    #instrument(FsManager)
     */
    public FsCompositeDriver instrument(
            InstrumentingManager<This> context,
            FsCompositeDriver subject) {
        return subject;
    }

    /**
     * Instruments the given {@code subject}.
     *
     * @param  context the instrumenting object which called this method.
     * @param  subject the subject of instrumentation.
     * @return An instrumenting object or {@code subject} if the implementation
     *         does not want to instrument it.
     * @see    #instrument(FsManager)
     */
    public FsController instrument(
            InstrumentingManager<This> context,
            FsController subject) {
        return subject;
    }

    /**
     * Instruments the given {@code subject}.
     *
     * @param  context the instrumenting object which called this method.
     * @param  subject the subject of instrumentation.
     * @return An instrumenting object or {@code subject} if the implementation
     *         does not want to instrument it.
     * @see    #instrument(IoBufferPool)
     */
    public IoBuffer instrument(
            InstrumentingBufferPool<This> context,
            IoBuffer subject) {
        return subject;
    }

    /**
     * Instruments the given {@code subject}.
     *
     * @param  context the instrumenting object which called this method.
     * @param  subject the subject of instrumentation.
     * @return An instrumenting object or {@code subject} if the implementation
     *         does not want to instrument it.
     * @see    #instrument(InstrumentingManager, FsCompositeDriver)
     */
    public FsModel instrument(
            InstrumentingCompositeDriver<This> context,
            FsModel subject) {
        return subject;
    }

    /**
     * Instruments the given {@code subject}.
     *
     * @param  context the instrumenting object which called this method.
     * @param  subject the subject of instrumentation.
     * @return An instrumenting object or {@code subject} if the implementation
     *         does not want to instrument it.
     * @see    #instrument(InstrumentingManager, FsCompositeDriver)
     */
    public FsController instrument(
            InstrumentingCompositeDriver<This> context,
            FsController subject) {
        return subject;
    }

    /**
     * Instruments the given {@code subject}.
     *
     * @param  <E> the type of the target entry for I/O operations.
     * @param  context the instrumenting object which called this method.
     * @param  subject the subject of instrumentation.
     * @return An instrumenting object or {@code subject} if the implementation
     *         does not want to instrument it.
     * @see    #instrument(InstrumentingManager, FsController)
     * @see    #instrument(InstrumentingCompositeDriver, FsController)
     */
    public <E extends Entry> InputSocket<E> instrument(
            InstrumentingController<This> context,
            InputSocket<E> subject) {
        return subject;
    }

    /**
     * Instruments the given {@code subject}.
     *
     * @param  <E> the type of the target entry for I/O operations.
     * @param  context the instrumenting object which called this method.
     * @param  subject the subject of instrumentation.
     * @return An instrumenting object or {@code subject} if the implementation
     *         does not want to instrument it.
     * @see    #instrument(InstrumentingManager, FsController)
     * @see    #instrument(InstrumentingCompositeDriver, FsController)
     */
    public <E extends Entry> OutputSocket<E> instrument(
            InstrumentingController<This> context,
            OutputSocket<E> subject) {
        return subject;
    }

    /**
     * Instruments the given {@code subject}.
     *
     * @param  <B> the type of the target entry for I/O operations.
     * @param  context the instrumenting object which called this method.
     * @param  subject the subject of instrumentation.
     * @return An instrumenting object or {@code subject} if the implementation
     *         does not want to instrument it.
     * @see    #instrument(InstrumentingBufferPool, IoBuffer)
     */
    public <B extends IoBuffer> InputSocket<B> instrument(
            InstrumentingBuffer<This> context,
            InputSocket<B> subject) {
        return subject;
    }

    /**
     * Instruments the given {@code subject}.
     *
     * @param  <B> the type of the target entry for I/O operations.
     * @param  context the instrumenting object which called this method.
     * @param  subject the subject of instrumentation.
     * @return An instrumenting object or {@code subject} if the implementation
     *         does not want to instrument it.
     * @see    #instrument(InstrumentingBufferPool, IoBuffer)
     */
    public <B extends IoBuffer> OutputSocket<B> instrument(
            InstrumentingBuffer<This> context,
            OutputSocket<B> subject) {
        return subject;
    }

    /**
     * Instruments the given {@code subject}.
     *
     * @param  <E> the type of the target entry for I/O operations.
     * @param  context the instrumenting object which called this method.
     * @param  subject the subject of instrumentation.
     * @return An instrumenting object or {@code subject} if the implementation
     *         does not want to instrument it.
     * @see    #instrument(InstrumentingController, InputSocket)
     * @see    #instrument(InstrumentingBuffer, InputSocket)
     */
    public <E extends Entry> InputStream instrument(
            InstrumentingInputSocket<This, E> context,
            InputStream subject) {
        return subject;
    }

    /**
     * Instruments the given {@code subject}.
     *
     * @param  <E> the type of the target entry for I/O operations.
     * @param  context the instrumenting object which called this method.
     * @param  subject the subject of instrumentation.
     * @return An instrumenting object or {@code subject} if the implementation
     *         does not want to instrument it.
     * @see    #instrument(InstrumentingController, InputSocket)
     * @see    #instrument(InstrumentingBuffer, InputSocket)
     */
    public <E extends Entry> SeekableByteChannel instrument(
            InstrumentingInputSocket<This, E> context,
            SeekableByteChannel subject) {
        return subject;
    }

    /**
     * Instruments the given {@code subject}.
     *
     * @param  <E> the type of the target entry for I/O operations.
     * @param  context the instrumenting object which called this method.
     * @param  subject the subject of instrumentation.
     * @return An instrumenting object or {@code subject} if the implementation
     *         does not want to instrument it.
     * @see    #instrument(InstrumentingController, OutputSocket)
     * @see    #instrument(InstrumentingBuffer, OutputSocket)
     */
    public <E extends Entry> OutputStream instrument(
            InstrumentingOutputSocket<This, E> context,
            OutputStream subject) {
        return subject;
    }

    /**
     * Instruments the given {@code subject}.
     *
     * @param  <E> the type of the target entry for I/O operations.
     * @param  context the instrumenting object which called this method.
     * @param  subject the subject of instrumentation.
     * @return An instrumenting object or {@code subject} if the implementation
     *         does not want to instrument it.
     * @see    #instrument(InstrumentingController, OutputSocket)
     * @see    #instrument(InstrumentingBuffer, OutputSocket)
     */
    public <E extends Entry> SeekableByteChannel instrument(
            InstrumentingOutputSocket<This, E> context,
            SeekableByteChannel subject) {
        return subject;
    }
}
