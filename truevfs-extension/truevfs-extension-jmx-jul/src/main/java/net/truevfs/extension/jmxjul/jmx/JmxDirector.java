/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.extension.jmxjul.jmx;

import java.lang.management.ManagementFactory;
import javax.annotation.concurrent.ThreadSafe;
import javax.management.*;
import net.truevfs.extension.jmxjul.*;
import net.truevfs.kernel.FsController;
import net.truevfs.kernel.FsManager;
import net.truevfs.kernel.FsModel;
import net.truevfs.kernel.cio.*;

/**
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class JmxDirector extends InstrumentingDirector<JmxDirector> {

    private static final String APPLICATION_IO_STATISTICS = "ApplicationIOStatistics";
    private static final String KERNEL_IO_STATISTICS = "KernelIOStatistics";
    private static final String TEMP_IO_STATISTICS = "TempIOStatistics";
    private static final MBeanServer
            mbs = ManagementFactory.getPlatformMBeanServer();
    public static final JmxDirector SINGLETON = new JmxDirector();

    /** Can't touch this - hammer time! */
    private JmxDirector() { }

    private volatile JmxIOStatistics application;

    JmxIOStatistics getApplicationIOStatistics() {
        final JmxIOStatistics stats = application;
        assert null != stats;
        return stats;
    }

    void setApplicationIOStatistics(final JmxIOStatistics stats) {
        assert null != stats;
        this.application = stats;
        JmxIOStatisticsView.register(stats, APPLICATION_IO_STATISTICS);
    }

    private volatile JmxIOStatistics kernel;

    JmxIOStatistics getKernelIOStatistics() {
        final JmxIOStatistics stats = kernel;
        assert null != stats;
        return stats;
    }

    void setKernelIOStatistics(final JmxIOStatistics stats) {
        assert null != stats;
        this.kernel = stats;
        JmxIOStatisticsView.register(stats, KERNEL_IO_STATISTICS);
    }

    private volatile JmxIOStatistics temp;

    JmxIOStatistics getTempIOStatistics() {
        final JmxIOStatistics stats = temp;
        assert null != stats;
        return stats;
    }

    void setTempIOStatistics(final JmxIOStatistics stats) {
        assert null != stats;
        this.temp = stats;
        JmxIOStatisticsView.register(stats, TEMP_IO_STATISTICS);
    }

    void clearStatistics() {
        for (final Object[] params : new Object[][] {
            { APPLICATION_IO_STATISTICS, application, },
            { KERNEL_IO_STATISTICS, kernel, },
            { TEMP_IO_STATISTICS, temp, },
        }) {
            final ObjectName pattern;
            try {
                pattern = new ObjectName(FsManager.class.getName() + ":type=" + params[0] + ",name=*");
            } catch (MalformedObjectNameException ex) {
                throw new AssertionError();
            }
            for (final ObjectName found : mbs.queryNames(pattern, null)) {
                final JmxIOStatisticsMXBean proxy = JMX.newMXBeanProxy(
                        mbs, found, JmxIOStatisticsMXBean.class);
                if (((JmxIOStatistics) params[1]).getTimeCreatedMillis()
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
    public FsManager instrument(FsManager manager) {
        return new JmxManager(this, manager);
    }

    @Override
    public <B extends IoBuffer<B>> IoPool<B> instrument(IoPool<B> pool) {
        return new JmxIOPool<B>(pool, this);
    }

    @Override
    protected FsModel instrument(
            FsModel model,
            InstrumentingCompositeDriver context) {
        return new JmxModel(this, model);
    }

    @Override
    protected FsController<? extends FsModel> instrument(
            FsController<? extends FsModel> controller,
            InstrumentingManager context) {
        return new JmxApplicationController(this, controller);
    }

    @Override
    protected FsController<? extends FsModel> instrument(
            FsController<? extends FsModel> controller,
            InstrumentingCompositeDriver context) {
        return new JmxKernelController(this, controller);
    }

    @Override
    protected <B extends IoBuffer<B>> InputSocket<B> instrument(
            InputSocket<B> input,
            InstrumentingIOPool<B>.InstrumentingBuffer context) {
        return new JmxInputSocket<B>(this, input, temp);
    }

    @Override
    protected <E extends Entry> InputSocket<E> instrument(
            InputSocket<E> input,
            InstrumentingController<JmxDirector> context) {
        return new JmxInputSocket<E>(this, input,
                JmxController.class.cast(context).getIOStatistics());
    }

    @Override
    protected <B extends IoBuffer<B>> OutputSocket<B> instrument(
            OutputSocket<B> output,
            InstrumentingIOPool<B>.InstrumentingBuffer context) {
        return new JmxOutputSocket<B>(this, output, temp);
    }

    @Override
    protected <E extends Entry> OutputSocket<E> instrument(
            OutputSocket<E> output,
            InstrumentingController<JmxDirector> context) {
        return new JmxOutputSocket<E>(this, output,
                JmxController.class.cast(context).getIOStatistics());
    }
}
