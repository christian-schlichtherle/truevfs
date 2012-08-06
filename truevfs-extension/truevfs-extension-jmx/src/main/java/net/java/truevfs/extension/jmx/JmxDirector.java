/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.extension.jmx;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.Hashtable;
import javax.annotation.concurrent.ThreadSafe;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import net.java.truecommons.shed.HashMaps;
import net.java.truevfs.component.instrumentation.*;
import net.java.truevfs.kernel.spec.FsController;
import net.java.truevfs.kernel.spec.FsManager;
import net.java.truevfs.kernel.spec.FsModel;
import net.java.truevfs.kernel.spec.cio.*;

/**
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class JmxDirector extends AbstractDirector<JmxDirector> {

    private static final String APPLICATION_IO_STATISTICS = "Application I/O Statistics";
    private static final String KERNEL_IO_STATISTICS = "Kernel I/O Statistics";
    private static final String BUFFER_IO_STATISTICS = "Buffer I/O Statistics";

    static final JmxDirector SINGLETON = new JmxDirector();

    private volatile JmxIoStatistics appStatistics, kernelStatistics, bufferStatistics;

    private JmxDirector() { }

    JmxIoStatistics getAppStatistics() {
        final JmxIoStatistics stats = appStatistics;
        assert null != stats;
        return stats;
    }

    void setAppStatistics(final JmxIoStatistics stats) {
        assert null != stats;
        this.appStatistics = stats;
        JmxIoStatisticsView.register(stats, APPLICATION_IO_STATISTICS);
    }

    JmxIoStatistics getKernelStatistics() {
        final JmxIoStatistics stats = kernelStatistics;
        assert null != stats;
        return stats;
    }

    void setKernelStatistics(final JmxIoStatistics stats) {
        assert null != stats;
        this.kernelStatistics = stats;
        JmxIoStatisticsView.register(stats, KERNEL_IO_STATISTICS);
    }

    JmxIoStatistics getBufferStatistics() {
        final JmxIoStatistics stats = bufferStatistics;
        assert null != stats;
        return stats;
    }

    void setBufferStatistics(final JmxIoStatistics stats) {
        assert null != stats;
        this.bufferStatistics = stats;
        JmxIoStatisticsView.register(stats, BUFFER_IO_STATISTICS);
    }

    void clearStatistics() {
        for (final Object[] params : new Object[][] {
            { APPLICATION_IO_STATISTICS, appStatistics, },
            { KERNEL_IO_STATISTICS, kernelStatistics, },
            { BUFFER_IO_STATISTICS, bufferStatistics, },
        }) {
            @SuppressWarnings("UseOfObsoleteCollectionType")
            final java.util.Hashtable<String, String>
                    table = new Hashtable<>(HashMaps.initialCapacity(2));
            table.put("type", (String) params[0]);
            table.put("time", "*");
            final ObjectName pattern;
            try {
                pattern = new ObjectName(
                        JmxIoStatisticsView.class.getPackage().getName(),
                        table);
            } catch (MalformedObjectNameException ex) {
                throw new AssertionError();
            }
            for (final ObjectName found : JmxUtils.queryNames(pattern)) {
                final JmxIoStatisticsMXBean proxy = JmxUtils
                        .newMXBeanProxy(found, JmxIoStatisticsMXBean.class);
                if (((JmxIoStatistics) params[1]).getTimeCreatedMillis()
                        != proxy.getTimeCreatedMillis())
                    JmxUtils.unregister(found);
            }
        }
    }

    @Override
    public FsManager instrument(FsManager object) {
        return new JmxManager(this, object);
    }

    @Override
    public IoBufferPool instrument(IoBufferPool object) {
        return new JmxIoBufferPool(this, object);
    }

    @Override
    public FsController instrument(
            InstrumentingManager<JmxDirector> origin,
            FsController controller) {
        return new JmxApplicationController(this, controller);
    }

    @Override
    public FsModel instrument(
            InstrumentingMetaDriver<JmxDirector> origin,
            FsModel object) {
        return new JmxModel(object);
    }

    @Override
    public FsController instrument(
            InstrumentingMetaDriver<JmxDirector> origin,
            FsController object) {
        return new JmxKernelController(this, object);
    }

    @Override
    public InputSocket<? extends Entry> instrument(
            InstrumentingController<JmxDirector> origin,
            InputSocket<? extends Entry> object) {
        return new JmxInputSocket<>(this, object,
                ((JmxWithIoStatistics) origin).getStats());
    }

    @Override
    public OutputSocket<? extends Entry> instrument(
            InstrumentingController<JmxDirector> origin,
            OutputSocket<? extends Entry> object) {
        return new JmxOutputSocket<>(this, object,
                ((JmxWithIoStatistics) origin).getStats());
    }

    @Override
    public InputSocket<? extends IoBuffer> instrument(
            InstrumentingIoBuffer<JmxDirector> origin,
            InputSocket<? extends IoBuffer> object) {
        return new JmxInputSocket<>(this, object, bufferStatistics);
    }

    @Override
    public OutputSocket<? extends IoBuffer> instrument(
            InstrumentingIoBuffer<JmxDirector> origin,
            OutputSocket<? extends IoBuffer> object) {
        return new JmxOutputSocket<>(this, object, bufferStatistics);
    }

    @Override
    public InputStream instrument(
            InstrumentingInputSocket<JmxDirector, ? extends Entry> origin,
            InputStream object) {
        return new JmxInputStream(object,
                ((JmxWithIoStatistics) origin).getStats());
    }

    @Override
    public SeekableByteChannel instrument(
            InstrumentingInputSocket<JmxDirector, ? extends Entry> origin,
            SeekableByteChannel object) {
        return new JmxSeekableChannel(object,
                ((JmxWithIoStatistics) origin).getStats());
    }

    @Override
    public OutputStream instrument(
            InstrumentingOutputSocket<JmxDirector, ? extends Entry> origin,
            OutputStream object) {
        return new JmxOutputStream(object,
                ((JmxWithIoStatistics) origin).getStats());
    }

    @Override
    public SeekableByteChannel instrument(
            InstrumentingOutputSocket<JmxDirector, ? extends Entry> origin,
            SeekableByteChannel object) {
        return new JmxSeekableChannel(object,
                ((JmxWithIoStatistics) origin).getStats());
    }
}
