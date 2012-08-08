/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx;

import net.java.truevfs.ext.jmx.model.IoStatistics;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.concurrent.ThreadSafe;
import net.java.truevfs.comp.inst.AbstractDirector;
import net.java.truevfs.comp.inst.InstrumentingBuffer;
import net.java.truevfs.comp.inst.InstrumentingBufferPool;
import net.java.truevfs.comp.inst.InstrumentingController;
import net.java.truevfs.comp.inst.InstrumentingInputSocket;
import net.java.truevfs.comp.inst.InstrumentingManager;
import net.java.truevfs.comp.inst.InstrumentingMetaDriver;
import net.java.truevfs.comp.inst.InstrumentingOutputSocket;
import net.java.truevfs.comp.jmx.JmxController;
import net.java.truevfs.comp.jmx.JmxNameBuilder;
import net.java.truevfs.kernel.spec.FsController;
import net.java.truevfs.kernel.spec.FsManager;
import net.java.truevfs.kernel.spec.FsModel;
import net.java.truevfs.kernel.spec.cio.Entry;
import net.java.truevfs.kernel.spec.cio.InputSocket;
import net.java.truevfs.kernel.spec.cio.IoBuffer;
import net.java.truevfs.kernel.spec.cio.OutputSocket;

/**
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class JmxDirector extends AbstractDirector<JmxDirector> {
    static final JmxDirector SINGLETON = new JmxDirector();

    private volatile IoStatistics application, kernel, buffer;

    private JmxDirector() { }

    private <C extends JmxController> C init(C controller) {
        controller.init();
        return controller;
    }

    IoStatistics getApplicationStatistics() {
        final IoStatistics stats = application;
        assert null != stats;
        return stats;
    }

    void setApplicationStatistics(final IoStatistics stats) {
        assert null != stats;
        this.application = stats;
        init(new JmxStatisticsController(this, stats));
    }

    IoStatistics getKernelStatistics() {
        final IoStatistics stats = kernel;
        assert null != stats;
        return stats;
    }

    void setKernelStatistics(final IoStatistics stats) {
        assert null != stats;
        this.kernel = stats;
        init(new JmxStatisticsController(this, stats));
    }

    IoStatistics getBufferStatistics() {
        final IoStatistics stats = buffer;
        assert null != stats;
        return stats;
    }

    void setBufferStatistics(final IoStatistics stats) {
        assert null != stats;
        this.buffer = stats;
        init(new JmxStatisticsController(this, stats));
    }

    @Override
    public final FsManager instrument(FsManager object) {
        return init(new JmxManagerController(this, object));
    }

    @Override
    public FsController instrument(
            InstrumentingManager<JmxDirector> origin,
            FsController object) {
        return init(new JmxApplicationController(this, object));
    }

    @Override
    public final IoBuffer instrument(
            InstrumentingBufferPool<JmxDirector> origin,
            IoBuffer object) {
        return init(new JmxBufferController(this, object));
    }

    @Override
    public final FsModel instrument(
            InstrumentingMetaDriver<JmxDirector> origin,
            FsModel object) {
        return init(new JmxModelController(this, object));
    }

    @Override
    public FsController instrument(
            InstrumentingMetaDriver<JmxDirector> origin,
            FsController object) {
        return init(new JmxKernelController(this, object));
    }

    @Override
    public InputSocket<? extends Entry> instrument(
            InstrumentingController<JmxDirector> origin,
            InputSocket<? extends Entry> object) {
        return init(new JmxInputSocket<>(this, object,
                ((JmxStatisticsProvider) origin).getStatistics()));
    }

    @Override
    public OutputSocket<? extends Entry> instrument(
            InstrumentingController<JmxDirector> origin,
            OutputSocket<? extends Entry> object) {
        return init(new JmxOutputSocket<>(this, object,
                ((JmxStatisticsProvider) origin).getStatistics()));
    }

    @Override
    public InputSocket<? extends IoBuffer> instrument(
            InstrumentingBuffer<JmxDirector> origin,
            InputSocket<? extends IoBuffer> object) {
        return init(new JmxInputSocket<>(this, object, buffer));
    }

    @Override
    public OutputSocket<? extends IoBuffer> instrument(
            InstrumentingBuffer<JmxDirector> origin,
            OutputSocket<? extends IoBuffer> object) {
        return init(new JmxOutputSocket<>(this, object, buffer));
    }

    @Override
    public InputStream instrument(
            InstrumentingInputSocket<JmxDirector, ? extends Entry> origin,
            InputStream object) {
        return init(new JmxInputStream(object,
                ((JmxStatisticsProvider) origin).getStatistics()));
    }

    @Override
    public SeekableByteChannel instrument(
            InstrumentingInputSocket<JmxDirector, ? extends Entry> origin,
            SeekableByteChannel object) {
        return init(new JmxSeekableChannel(object,
                ((JmxStatisticsProvider) origin).getStatistics()));
    }

    @Override
    public OutputStream instrument(
            InstrumentingOutputSocket<JmxDirector, ? extends Entry> origin,
            OutputStream object) {
        return init(new JmxOutputStream(object,
                ((JmxStatisticsProvider) origin).getStatistics()));
    }

    @Override
    public SeekableByteChannel instrument(
            InstrumentingOutputSocket<JmxDirector, ? extends Entry> origin,
            SeekableByteChannel object) {
        return init(new JmxSeekableChannel(object,
                ((JmxStatisticsProvider) origin).getStatistics()));
    }

    public JmxNameBuilder nameBuilder(Class<?> type) {
        return new JmxNameBuilder(getDomain())
                .put("type", type.getSimpleName());
    }

    private Package getDomain() {
        return getClass().getPackage();
    }
}
