/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.component.instrumentation;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import net.java.truevfs.kernel.spec.FsController;
import net.java.truevfs.kernel.spec.FsManager;
import net.java.truevfs.kernel.spec.FsMetaDriver;
import net.java.truevfs.kernel.spec.FsModel;
import net.java.truevfs.kernel.spec.cio.*;

/**
 * @param  <This> the type of this director.
 * @author Christian Schlichtherle
 */
public abstract class AbstractDirector<This extends Director<This>>
implements Director<This> {

    @Override
    @SuppressWarnings("unchecked")
    public FsManager instrument(FsManager object) {
        return new InstrumentingManager<>((This) this, object);
    }

    @Override
    @SuppressWarnings("unchecked")
    public IoBufferPool instrument(IoBufferPool object) {
        return new InstrumentingIoBufferPool<>((This) this, object);
    }

    @Override
    @SuppressWarnings("unchecked")
    public FsMetaDriver instrument(
            InstrumentingManager<This> origin,
            FsMetaDriver object) {
        return new InstrumentingMetaDriver<>((This) this, object);
    }

    @Override
    public FsController instrument(
            InstrumentingManager<This> origin,
            FsController object) {
        return object;
    }

    @Override
    @SuppressWarnings("unchecked")
    public IoBuffer instrument(
            InstrumentingIoBufferPool<This> origin,
            IoBuffer object) {
        return new InstrumentingIoBuffer<>((This) this, object);
    }

    @Override
    public FsModel instrument(
            InstrumentingMetaDriver<This> origin,
            FsModel object) {
        return object;
    }

    @Override
    @SuppressWarnings("unchecked")
    public FsController instrument(
            InstrumentingMetaDriver<This> origin,
            FsController object) {
        return new InstrumentingController<>((This) this, object);
    }

    @Override
    @SuppressWarnings("unchecked")
    public InputSocket<? extends Entry> instrument(
            InstrumentingController<This> origin,
            InputSocket<? extends Entry> object) {
        return new InstrumentingInputSocket<>((This) this, object);
    }

    @Override
    @SuppressWarnings("unchecked")
    public OutputSocket<? extends Entry> instrument(
            InstrumentingController<This> origin,
            OutputSocket<? extends Entry> object) {
        return new InstrumentingOutputSocket<>((This) this, object);
    }

    @Override
    @SuppressWarnings("unchecked")
    public InputSocket<? extends IoBuffer> instrument(
            InstrumentingIoBuffer<This> origin,
            InputSocket<? extends IoBuffer> object) {
        return new InstrumentingInputSocket<>((This) this, object);
    }

    @Override
    @SuppressWarnings("unchecked")
    public OutputSocket<? extends IoBuffer> instrument(
            InstrumentingIoBuffer<This> origin,
            OutputSocket<? extends IoBuffer> object) {
        return new InstrumentingOutputSocket<>((This) this, object);
    }

    @Override
    public InputStream instrument(
            InstrumentingInputSocket<This, ? extends Entry> origin,
            InputStream object) {
        return object;
    }

    @Override
    public SeekableByteChannel instrument(
            InstrumentingInputSocket<This, ? extends Entry> origin,
            SeekableByteChannel object) {
        return object;
    }

    @Override
    public OutputStream instrument(
            InstrumentingOutputSocket<This, ? extends Entry> origin,
            OutputStream object) {
        return object;
    }

    @Override
    public SeekableByteChannel instrument(
            InstrumentingOutputSocket<This, ? extends Entry> origin,
            SeekableByteChannel object) {
        return object;
    }
}
