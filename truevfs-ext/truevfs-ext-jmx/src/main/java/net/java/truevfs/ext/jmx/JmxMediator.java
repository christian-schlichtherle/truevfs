/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.concurrent.atomic.AtomicReferenceArray;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Provider;
import net.java.truevfs.comp.inst.InstrumentingBuffer;
import net.java.truevfs.comp.inst.InstrumentingBufferPool;
import net.java.truevfs.comp.inst.InstrumentingController;
import net.java.truevfs.comp.inst.InstrumentingInputSocket;
import net.java.truevfs.comp.inst.InstrumentingManager;
import net.java.truevfs.comp.inst.InstrumentingMetaDriver;
import net.java.truevfs.comp.inst.InstrumentingOutputSocket;
import net.java.truevfs.comp.inst.Mediator;
import net.java.truevfs.comp.jmx.JmxObjectNameBuilder;
import static net.java.truevfs.ext.jmx.JmxStatisticsKind.*;
import net.java.truevfs.ext.jmx.model.IoLogger;
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
public class JmxMediator extends Mediator<JmxMediator> {
    static final JmxMediator SINGLETON = new JmxMediator();

    private final AtomicReferenceArray<IoLogger> loggers;

    private JmxMediator() {
        final JmxStatisticsKind[] kinds = JmxStatisticsKind.values();
        loggers = new AtomicReferenceArray<>(kinds.length);
        for (final JmxStatisticsKind kind : kinds)
            loggers.set(kind.ordinal(), new IoLogger());
            
    }

    /**
     * {@linkplain JmxColleague#start Starts} and returns the given
     * {@code colleague}.
     * 
     * @param  <C> the type of the colleague to start.
     * @param  colleague the colleague to start.
     * @return The started colleague.
     */
    protected final <C extends JmxColleague> C start(C colleague) {
        colleague.start();
        return colleague;
    }

    final IoLogger getLogger(JmxStatisticsKind kind) {
        return loggers.get(kind.ordinal());
    }

    final void nextLoggers() {
        for (JmxStatisticsKind kind : JmxStatisticsKind.values()) {
            final int ordinal = kind.ordinal();
            while (true) {
                final IoLogger expected = loggers.get(ordinal);
                final IoLogger updated = expected.next();
                if (loggers.weakCompareAndSet(ordinal, expected, updated)) {
                    start(new JmxStatistics(this, kind, updated));
                    break;
                }
            }
        }
    }

    @Override
    public final FsManager instrument(FsManager object) {
        return start(new JmxManager(this, object));
    }

    @Override
    public FsController instrument(
            InstrumentingManager<JmxMediator> origin,
            FsController object) {
        return start(new JmxApplicationController(this, object));
    }

    @Override
    public final IoBuffer instrument(
            InstrumentingBufferPool<JmxMediator> origin,
            IoBuffer object) {
        return start(new JmxBuffer(this, object));
    }

    @Override
    public final FsModel instrument(
            InstrumentingMetaDriver<JmxMediator> origin,
            FsModel object) {
        return start(new JmxModel(this, object));
    }

    @Override
    public FsController instrument(
            InstrumentingMetaDriver<JmxMediator> origin,
            FsController object) {
        return start(new JmxKernelController(this, object));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E extends Entry> InputSocket<E> instrument(
            InstrumentingController<JmxMediator> origin,
            InputSocket<E> object) {
        return start(new JmxInputSocket<>(this, object,
                ((Provider<JmxStatisticsKind>) origin).get()));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E extends Entry> OutputSocket<E> instrument(
            InstrumentingController<JmxMediator> origin,
            OutputSocket<E> object) {
        return start(new JmxOutputSocket<>(this, object,
                ((Provider<JmxStatisticsKind>) origin).get()));
    }

    @Override
    public <B extends IoBuffer> InputSocket<B> instrument(
            InstrumentingBuffer<JmxMediator> origin,
            InputSocket<B> object) {
        return start(new JmxInputSocket<>(this, object, BUFFER));
    }

    @Override
    public <B extends IoBuffer> OutputSocket<B> instrument(
            InstrumentingBuffer<JmxMediator> origin,
            OutputSocket<B> object) {
        return start(new JmxOutputSocket<>(this, object, BUFFER));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E extends Entry> InputStream instrument(
            InstrumentingInputSocket<JmxMediator, E> origin,
            InputStream object) {
        return start(new JmxInputStream(object,
                getLogger(((Provider<JmxStatisticsKind>) origin).get())));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E extends Entry> SeekableByteChannel instrument(
            InstrumentingInputSocket<JmxMediator, E> origin,
            SeekableByteChannel object) {
        return start(new JmxSeekableChannel(object,
                getLogger(((Provider<JmxStatisticsKind>) origin).get())));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E extends Entry> OutputStream instrument(
            InstrumentingOutputSocket<JmxMediator, E> origin,
            OutputStream object) {
        return start(new JmxOutputStream(object,
                getLogger(((Provider<JmxStatisticsKind>) origin).get())));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E extends Entry> SeekableByteChannel instrument(
            InstrumentingOutputSocket<JmxMediator, E> origin,
            SeekableByteChannel object) {
        return start(new JmxSeekableChannel(object,
                getLogger(((Provider<JmxStatisticsKind>) origin).get())));
    }

    public JmxObjectNameBuilder nameBuilder(Class<?> type) {
        return new JmxObjectNameBuilder(getDomain())
                .put("type", type.getSimpleName());
    }

    private Package getDomain() {
        return getClass().getPackage();
    }
}
