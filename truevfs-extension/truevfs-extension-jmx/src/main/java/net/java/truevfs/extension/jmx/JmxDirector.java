/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.extension.jmx;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.concurrent.ThreadSafe;
import javax.management.*;
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
    private static final MBeanServer
            mbs = ManagementFactory.getPlatformMBeanServer();

    static final JmxDirector SINGLETON = new JmxDirector();

    private volatile JmxIoStatistics appStats, kernelStats, bufferStats;

    private JmxDirector() { }

    JmxIoStatistics getAppStats() {
        final JmxIoStatistics stats = appStats;
        assert null != stats;
        return stats;
    }

    void setAppStats(final JmxIoStatistics stats) {
        assert null != stats;
        this.appStats = stats;
        JmxIoStatisticsView.register(stats, APPLICATION_IO_STATISTICS);
    }

    JmxIoStatistics getKernelStats() {
        final JmxIoStatistics stats = kernelStats;
        assert null != stats;
        return stats;
    }

    void setKernelStats(final JmxIoStatistics stats) {
        assert null != stats;
        this.kernelStats = stats;
        JmxIoStatisticsView.register(stats, KERNEL_IO_STATISTICS);
    }

    JmxIoStatistics getBufferStats() {
        final JmxIoStatistics stats = bufferStats;
        assert null != stats;
        return stats;
    }

    void setBufferStats(final JmxIoStatistics stats) {
        assert null != stats;
        this.bufferStats = stats;
        JmxIoStatisticsView.register(stats, BUFFER_IO_STATISTICS);
    }

    void clearStats() {
        for (final Object[] params : new Object[][] {
            { APPLICATION_IO_STATISTICS, appStats, },
            { KERNEL_IO_STATISTICS, kernelStats, },
            { BUFFER_IO_STATISTICS, bufferStats, },
        }) {
            final ObjectName pattern;
            try {
                pattern = new ObjectName(FsManager.class.getName() + ":type=" + params[0] + ",name=*");
            } catch (MalformedObjectNameException ex) {
                throw new AssertionError();
            }
            for (final ObjectName found : mbs.queryNames(pattern, null)) {
                final JmxIoStatisticsMXBean proxy = JMX.newMXBeanProxy(
                        mbs, found, JmxIoStatisticsMXBean.class);
                if (((JmxIoStatistics) params[1]).getTimeCreatedMillis()
                        == proxy.getTimeCreatedMillis())
                    continue;
                try {
                    mbs.unregisterMBean(found);
                } catch (InstanceNotFoundException ex) {
                    throw new AssertionError();
                } catch (MBeanRegistrationException ex) {
                    throw new AssertionError(ex);
                }
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
        return new JmxInputSocket<>(this, object, bufferStats);
    }

    @Override
    public OutputSocket<? extends IoBuffer> instrument(
            InstrumentingIoBuffer<JmxDirector> origin,
            OutputSocket<? extends IoBuffer> object) {
        return new JmxOutputSocket<>(this, object, bufferStats);
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
