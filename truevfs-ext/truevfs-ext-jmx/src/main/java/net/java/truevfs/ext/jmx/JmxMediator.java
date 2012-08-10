/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.concurrent.ThreadSafe;
import net.java.truevfs.comp.inst.InstrumentingBuffer;
import net.java.truevfs.comp.inst.InstrumentingBufferPool;
import net.java.truevfs.comp.inst.InstrumentingController;
import net.java.truevfs.comp.inst.InstrumentingInputSocket;
import net.java.truevfs.comp.inst.InstrumentingManager;
import net.java.truevfs.comp.inst.InstrumentingMetaDriver;
import net.java.truevfs.comp.inst.InstrumentingOutputSocket;
import net.java.truevfs.comp.inst.Mediator;
import net.java.truevfs.comp.jmx.JmxObjectNameBuilder;
import net.java.truevfs.ext.jmx.stats.FsStatistics;
import net.java.truevfs.ext.jmx.stats.IoStatistics;
import net.java.truevfs.ext.jmx.stats.SyncStatistics;
import net.java.truevfs.kernel.spec.FsController;
import net.java.truevfs.kernel.spec.FsManager;
import net.java.truevfs.kernel.spec.FsModel;
import net.java.truevfs.kernel.spec.cio.Entry;
import net.java.truevfs.kernel.spec.cio.InputSocket;
import net.java.truevfs.kernel.spec.cio.IoBuffer;
import net.java.truevfs.kernel.spec.cio.OutputSocket;

/**
 * A mediator for the instrumentation of the TrueVFS Kernel with JMX.
 * Each instance of this class manages its own
 * {@linkplain #stats() file system statistics}.
 * 
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class JmxMediator extends Mediator<JmxMediator> {
    public static final JmxMediator APPLICATION = new JmxMediator("Application");
    public static final JmxMediator BUFFERS = new JmxMediator("Buffers");
    public static final JmxMediator KERNEL = new JmxMediator("Kernel");

    private final AtomicReference<FsStatistics> stats =
            new AtomicReference<>(FsStatistics.zero());
    private String subject;

    protected JmxMediator(final String subject) {
        this.subject = Objects.requireNonNull(subject);
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

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return String.format("%s[subject=%s]",
                getClass().getName(), getSubject());
    }

    public String getSubject() {
        return subject;
    }

    private AtomicReference<FsStatistics> getReadStatsRef() {
        return stats;
    }

    private AtomicReference<FsStatistics> getWriteStatsRef() {
        return stats;
    }

    private AtomicReference<FsStatistics> getSyncStatsRef() {
        return APPLICATION.stats;
    }

    public IoStatistics getReadStats() {
        return getReadStatsRef().get().getReadStats();
    }

    public IoStatistics getWriteStats() {
        return getWriteStatsRef().get().getWriteStats();
    }

    public SyncStatistics getSyncStats() {
        return getSyncStatsRef().get().getSyncStats();
    }

    /**
     * Logs a read operation with the given sample data and returns a new
     * object to reflect the updated statistics.
     * The sequence number of the returned object will be incremented and may
     * eventually overflow to zero.
     * 
     * @param  nanos the execution time in nanoseconds.
     * @param  bytes the number of bytes read.
     * @return A new object which reflects the updated statistics.
     * @throws IllegalArgumentException if any parameter value is negative.
     */
    public final FsStatistics logRead(long nanos, int bytes) {
        final AtomicReference<FsStatistics> ref = getReadStatsRef();
        while (true) {
            final FsStatistics expected = ref.get();
            final FsStatistics updated = expected.logRead(nanos, bytes);
            if (ref.weakCompareAndSet(expected, updated)) return updated;
        }
    }

    /**
     * Logs a write operation with the given sample data and returns a new
     * object to reflect the updated statistics.
     * The sequence number of the returned object will be incremented and may
     * eventually overflow to zero.
     * 
     * @param  nanos the execution time in nanoseconds.
     * @param  bytes the number of bytes written.
     * @return A new object which reflects the updated statistics.
     * @throws IllegalArgumentException if any parameter is negative.
     */
    public final FsStatistics logWrite(long nanos, int bytes) {
        final AtomicReference<FsStatistics> ref = getWriteStatsRef();
        while (true) {
            final FsStatistics expected = ref.get();
            final FsStatistics updated = expected.logWrite(nanos, bytes);
            if (ref.weakCompareAndSet(expected, updated)) return updated;
        }
    }

    /**
     * Logs a getSyncStats operation with the given sample data and returns a new
     * object to reflect the updated statistics.
     * The sequence number of the returned object will be incremented and may
     * eventually overflow to zero.
     * 
     * @param  nanos the execution time in nanoseconds.
     * @return A new object which reflects the updated statistics.
     * @throws IllegalArgumentException if any parameter value is negative.
     */
    public final FsStatistics logSync(long nanos) {
        final AtomicReference<FsStatistics> ref = getSyncStatsRef();
        while (true) {
            final FsStatistics expected = ref.get();
            final FsStatistics updated = expected.logSync(nanos);
            if (ref.weakCompareAndSet(expected, updated)) return updated;
        }
    }

    public void rotateStats() {
        for (JmxMediator mediator : allMediators()) mediator.nextStatistics();
    }

    protected JmxMediator[] allMediators() {
        return new JmxMediator[] { APPLICATION, KERNEL, BUFFERS };
    }

    private void nextStatistics() {
        start(newStatistics());
    }

    protected JmxStatistics newStatistics() {
        return new JmxStatistics(this);
    }

    @Override
    public final FsManager instrument(FsManager object) {
        return start(new JmxManager(this, object));
    }

    @Override
    public FsController instrument(
            InstrumentingManager<JmxMediator> origin,
            FsController object) {
        return start(new JmxController(APPLICATION, object)); // switch mediator!
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
        return start(new JmxController(KERNEL, object)); // switch mediator!
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E extends Entry> InputSocket<E> instrument(
            InstrumentingController<JmxMediator> origin,
            InputSocket<E> object) {
        return start(new JmxInputSocket<>(this, object));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E extends Entry> OutputSocket<E> instrument(
            InstrumentingController<JmxMediator> origin,
            OutputSocket<E> object) {
        return start(new JmxOutputSocket<>(this, object));
    }

    @Override
    public <B extends IoBuffer> InputSocket<B> instrument(
            InstrumentingBuffer<JmxMediator> origin,
            InputSocket<B> object) {
        return start(new JmxInputSocket<>(this, object));
    }

    @Override
    public <B extends IoBuffer> OutputSocket<B> instrument(
            InstrumentingBuffer<JmxMediator> origin,
            OutputSocket<B> object) {
        return start(new JmxOutputSocket<>(this, object));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E extends Entry> InputStream instrument(
            InstrumentingInputSocket<JmxMediator, E> origin,
            InputStream object) {
        return start(new JmxInputStream(this, object));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E extends Entry> SeekableByteChannel instrument(
            InstrumentingInputSocket<JmxMediator, E> origin,
            SeekableByteChannel object) {
        return start(new JmxSeekableChannel(this, object));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E extends Entry> OutputStream instrument(
            InstrumentingOutputSocket<JmxMediator, E> origin,
            OutputStream object) {
        return start(new JmxOutputStream(this, object));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E extends Entry> SeekableByteChannel instrument(
            InstrumentingOutputSocket<JmxMediator, E> origin,
            SeekableByteChannel object) {
        return start(new JmxSeekableChannel(this, object));
    }

    public JmxObjectNameBuilder nameBuilder(Class<?> type) {
        return new JmxObjectNameBuilder(getDomain())
                .put("type", type.getSimpleName());
    }

    private Package getDomain() {
        return getClass().getPackage();
    }
}
