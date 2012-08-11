/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.Objects;
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
import net.java.truevfs.ext.jmx.stats.FsLogger;
import net.java.truevfs.ext.jmx.stats.FsStatistics;
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
 * {@linkplain #getStats() file system statistics}.
 * 
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class JmxMediator extends Mediator<JmxMediator> {

    public static final JmxMediator APPLICATION_IO = new JmxMediator("Application I/O");
    public static final JmxMediator BUFFER_IO = new JmxMediator("Buffer I/O");
    public static final JmxMediator KERNEL_IO = new JmxMediator("Kernel I/O");

    private final FsLogger logger = new FsLogger();
    private String subject;

    protected JmxMediator(final String subject) {
        this.subject = Objects.requireNonNull(subject);
    }

    public String getSubject() { return subject; }

    int getLoggerSize() { return logger.size(); }

    String formatLoggerOffset(int offset) { return logger.format(offset); }

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

    private FsLogger getIoLogger() { return logger; }

    private FsLogger getSyncLogger() { return APPLICATION_IO.logger; }

    public FsStatistics getIoStats(int offset) {
        return getIoLogger().getStats(offset);
    }

    public FsStatistics getSyncStats(int offset) {
        return getSyncLogger().getStats(offset);
    }

    /**
     * Logs a read operation with the given sample data.
     * 
     * @param  nanos the execution time in nanoseconds.
     * @param  bytes the number of bytes read.
     * @throws IllegalArgumentException if any parameter value is negative.
     */
    public void logRead(long nanos, int bytes) {
        getIoLogger().logRead(nanos, bytes);
    }

    /**
     * Logs a write operation with the given sample data.
     * 
     * @param  nanos the execution time in nanoseconds.
     * @param  bytes the number of bytes written.
     * @throws IllegalArgumentException if any parameter is negative.
     */
    public void logWrite(long nanos, int bytes) {
        getIoLogger().logWrite(nanos, bytes);
    }

    /**
     * Logs a sync operation with the given sample data.
     * 
     * @param  nanos the execution time in nanoseconds.
     * @throws IllegalArgumentException if any parameter value is negative.
     */
    public void logSync(long nanos) {
        getSyncLogger().logSync(nanos);
    }

    protected JmxIoStatistics newIoStatistics(int offset) {
        return new JmxIoStatistics(this, offset);
    }

    protected JmxSyncStatistics newSyncStatistics(int offset) {
        return new JmxSyncStatistics(this, offset);
    }
    
    private void startStatistics(final int offset) {
        start(newIoStatistics(offset));
        start(newSyncStatistics(offset));
    }

    protected JmxMediator[] allMediators() {
        return new JmxMediator[] { APPLICATION_IO, KERNEL_IO, BUFFER_IO };
    }

    public void startStatistics() {
        for (JmxMediator mediator : allMediators())
            mediator.startStatistics(0);
    }

    private int rotateLogger() { return logger.rotate(); }

    public void rotateStatistics() {
        for (final JmxMediator mediator : allMediators())
            mediator.startStatistics(mediator.rotateLogger());
    }

    @Override
    public final FsManager instrument(FsManager object) {
        return start(new JmxManager(this, object));
    }

    @Override
    public FsController instrument(
            InstrumentingManager<JmxMediator> origin,
            FsController object) {
        return start(new JmxController(APPLICATION_IO, object)); // switch mediator!
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
        return start(new JmxController(KERNEL_IO, object)); // switch mediator!
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

    private Package getDomain() { return getClass().getPackage(); }
}
