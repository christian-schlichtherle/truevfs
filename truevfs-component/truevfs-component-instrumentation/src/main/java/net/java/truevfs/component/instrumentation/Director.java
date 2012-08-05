/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.component.instrumentation;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import net.java.truevfs.kernel.spec.FsCompositeDriver;
import net.java.truevfs.kernel.spec.FsController;
import net.java.truevfs.kernel.spec.FsManager;
import net.java.truevfs.kernel.spec.FsModel;
import net.java.truevfs.kernel.spec.cio.*;

/**
 * A mediator for instrumenting all objects which are used by the TrueVFS
 * Kernel.
 * Implementations are expected to either decorate a given object with an
 * instrumenting object of the same type or simply return the given object if
 * they prefer not to instrument it.
 *
 * @param <This> the type of this director.
 * @author Christian Schlichtherle
 */
public interface Director<This extends Director<This>> {

    /**
     * Instruments the given {@code object}.
     * 
     * @param  object the object to instrument.
     * @return An instrumenting object or {@code object} if the implementation
     *         does not want to instrument it.
     */
    FsManager instrument(FsManager object);

    /**
     * Instruments the given {@code object}.
     * 
     * @param  object the object to instrument.
     * @return An instrumenting object or {@code object} if the implementation
     *         does not want to instrument it.
     */
    IoBufferPool instrument(IoBufferPool object);

    /**
     * Instruments the given {@code object}.
     * 
     * @param  origin the instrumenting object which called this method.
     * @param  object the object to instrument.
     * @return An instrumenting object or {@code object} if the implementation
     *         does not want to instrument it.
     * @see    #instrument(FsManager)
     */
    FsCompositeDriver instrument(
            InstrumentingManager<This> origin,
            FsCompositeDriver object);

    /**
     * Instruments the given {@code object}.
     * 
     * @param  origin the instrumenting object which called this method.
     * @param  object the object to instrument.
     * @return An instrumenting object or {@code object} if the implementation
     *         does not want to instrument it.
     * @see    #instrument(FsManager)
     */
    FsController instrument(
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
    IoBuffer instrument(
            InstrumentingIoBufferPool<This> origin,
            IoBuffer object);

    /**
     * Instruments the given {@code object}.
     * 
     * @param  origin the instrumenting object which called this method.
     * @param  object the object to instrument.
     * @return An instrumenting object or {@code object} if the implementation
     *         does not want to instrument it.
     * @see    #instrument(FsManager, FsCompositeDriver)
     */
    FsModel instrument(
            InstrumentingCompositeDriver<This> origin,
            FsModel object);

    /**
     * Instruments the given {@code object}.
     * 
     * @param  origin the instrumenting object which called this method.
     * @param  object the object to instrument.
     * @return An instrumenting object or {@code object} if the implementation
     *         does not want to instrument it.
     * @see    #instrument(FsManager, FsCompositeDriver)
     */
    FsController instrument(
            InstrumentingCompositeDriver<This> origin,
            FsController object);

    /**
     * Instruments the given {@code object}.
     * 
     * @param  origin the instrumenting object which called this method.
     * @param  object the object to instrument.
     * @return An instrumenting object or {@code object} if the implementation
     *         does not want to instrument it.
     * @see    #instrument(FsManager, FsController)
     * @see    #instrument(FsCompositeDriver, FsController)
     */
    InputSocket<? extends Entry> instrument(
            InstrumentingController<This> origin,
            InputSocket<? extends Entry> object);

    /**
     * Instruments the given {@code object}.
     * 
     * @param  origin the instrumenting object which called this method.
     * @param  object the object to instrument.
     * @return An instrumenting object or {@code object} if the implementation
     *         does not want to instrument it.
     * @see    #instrument(FsManager, FsController)
     * @see    #instrument(FsCompositeDriver, FsController)
     */
    OutputSocket<? extends Entry> instrument(
            InstrumentingController<This> origin,
            OutputSocket<? extends Entry> object);

    /**
     * Instruments the given {@code object}.
     * 
     * @param  origin the instrumenting object which called this method.
     * @param  object the object to instrument.
     * @return An instrumenting object or {@code object} if the implementation
     *         does not want to instrument it.
     * @see    #instrument(IoBufferPool, IoBuffer)
     */
    InputSocket<? extends IoBuffer> instrument(
            InstrumentingIoBuffer<This> origin,
            InputSocket<? extends IoBuffer> object);

    /**
     * Instruments the given {@code object}.
     * 
     * @param  origin the instrumenting object which called this method.
     * @param  object the object to instrument.
     * @return An instrumenting object or {@code object} if the implementation
     *         does not want to instrument it.
     * @see    #instrument(IoBufferPool, IoBuffer)
     */
    OutputSocket<? extends IoBuffer> instrument(
            InstrumentingIoBuffer<This> origin,
            OutputSocket<? extends IoBuffer> object);

    /**
     * Instruments the given {@code object}.
     * 
     * @param  origin the instrumenting object which called this method.
     * @param  object the object to instrument.
     * @return An instrumenting object or {@code object} if the implementation
     *         does not want to instrument it.
     * @see    #instrument(FsController, InputSocket)
     * @see    #instrument(IoBuffer, InputSocket)
     */
    InputStream instrument(
            InstrumentingInputSocket<This, ? extends Entry> origin,
            InputStream object);

    /**
     * Instruments the given {@code object}.
     * 
     * @param  origin the instrumenting object which called this method.
     * @param  object the object to instrument.
     * @return An instrumenting object or {@code object} if the implementation
     *         does not want to instrument it.
     * @see    #instrument(FsController, InputSocket)
     * @see    #instrument(IoBuffer, InputSocket)
     */
    SeekableByteChannel instrument(
            InstrumentingInputSocket<This, ? extends Entry> origin,
            SeekableByteChannel object);

    /**
     * Instruments the given {@code object}.
     * 
     * @param  origin the instrumenting object which called this method.
     * @param  object the object to instrument.
     * @return An instrumenting object or {@code object} if the implementation
     *         does not want to instrument it.
     * @see    #instrument(FsController, OutputSocket)
     * @see    #instrument(IoBuffer, OutputSocket)
     */
    OutputStream instrument(
            InstrumentingOutputSocket<This, ? extends Entry> origin,
            OutputStream object);

    /**
     * Instruments the given {@code object}.
     * 
     * @param  origin the instrumenting object which called this method.
     * @param  object the object to instrument.
     * @return An instrumenting object or {@code object} if the implementation
     *         does not want to instrument it.
     * @see    #instrument(FsController, OutputSocket)
     * @see    #instrument(IoBuffer, OutputSocket)
     */
    SeekableByteChannel instrument(
            InstrumentingOutputSocket<This, ? extends Entry> origin,
            SeekableByteChannel object);
}
