/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.extension.jmxjul.jmx;

import de.schlichtherle.truezip.cio.*;
import de.schlichtherle.truezip.fs.FsController;
import de.schlichtherle.truezip.fs.FsManager;
import de.schlichtherle.truezip.fs.FsModel;
import de.truezip.extension.jmxjul.*;
import de.schlichtherle.truezip.util.JSE7;
import java.lang.management.ManagementFactory;
import javax.annotation.concurrent.ThreadSafe;
import javax.management.*;

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
    public static final JmxDirector SINGLETON = JSE7.AVAILABLE
            ? new JmxNio2Director()
            : new JmxDirector();

    private JmxDirector() {
    }

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
    public <B extends IOBuffer<B>> IOPool<B> instrument(IOPool<B> pool) {
        return new JmxIOPool<B>(pool, this);
    }

    @Override
    public FsManager instrument(FsManager manager) {
        return new JmxManager(manager, this);
    }

    @Override
    public FsModel instrument(
            FsModel model,
            InstrumentingCompositeDriver context) {
        return new JmxModel(model, this);
    }

    @Override
    public FsController<?> instrument(
            FsController<?> controller,
            InstrumentingManager context) {
        return new JmxApplicationController(controller, this);
    }

    @Override
    public FsController<?> instrument(
            FsController<?> controller,
            InstrumentingCompositeDriver context) {
        return new JmxKernelController(controller, this);
    }

    @Override
    public <B extends IOBuffer<B>> InputSocket<B> instrument(
            InputSocket<B> input,
            InstrumentingIOPool<B>.InstrumentingBuffer context) {
        return new JmxInputSocket<B>(input, this, temp);
    }

    @Override
    public <E extends Entry> InputSocket<E> instrument(
            InputSocket<E> input,
            InstrumentingController<JmxDirector> context) {
        return new JmxInputSocket<E>(input, this,
                JmxController.class.cast(context).getIOStatistics());
    }

    @Override
    public <B extends IOBuffer<B>> OutputSocket<B> instrument(
            OutputSocket<B> output,
            InstrumentingIOPool<B>.InstrumentingBuffer context) {
        return new JmxOutputSocket<B>(output, this, temp);
    }

    @Override
    public <E extends Entry> OutputSocket<E> instrument(
            OutputSocket<E> output,
            InstrumentingController<JmxDirector> context) {
        return new JmxOutputSocket<E>(output, this,
                JmxController.class.cast(context).getIOStatistics());
    }

    private static final class JmxNio2Director extends JmxDirector {
        @Override
        public <B extends IOBuffer<B>> InputSocket<B> instrument(
                InputSocket<B> input,
                InstrumentingIOPool<B>.InstrumentingBuffer context) {
            return new JmxNio2InputSocket<B>(input, this, super.temp);
        }

        @Override
        public <E extends Entry> InputSocket<E> instrument(
                InputSocket<E> input,
                InstrumentingController<JmxDirector> context) {
            return new JmxNio2InputSocket<E>(input, this,
                    JmxController.class.cast(context).getIOStatistics());
        }

        @Override
        public <B extends IOBuffer<B>> OutputSocket<B> instrument(
                OutputSocket<B> output,
                InstrumentingIOPool<B>.InstrumentingBuffer context) {
            return new JmxNio2OutputSocket<B>(output, this, super.temp);
        }

        @Override
        public <E extends Entry> OutputSocket<E> instrument(
                OutputSocket<E> output,
                InstrumentingController<JmxDirector> context) {
            return new JmxNio2OutputSocket<E>(output, this,
                    JmxController.class.cast(context).getIOStatistics());
        }
    }
}