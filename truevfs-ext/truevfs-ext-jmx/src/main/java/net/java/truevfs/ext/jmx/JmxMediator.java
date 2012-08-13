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
import net.java.truevfs.comp.inst.*;
import net.java.truevfs.comp.jmx.JmxBuffer;
import net.java.truevfs.comp.jmx.JmxColleague;
import net.java.truevfs.comp.jmx.JmxModel;
import net.java.truevfs.ext.jmx.stats.FsLogger;
import net.java.truevfs.ext.jmx.stats.FsStatistics;
import net.java.truevfs.kernel.spec.FsController;
import net.java.truevfs.kernel.spec.FsManager;
import net.java.truevfs.kernel.spec.FsMetaDriver;
import net.java.truevfs.kernel.spec.FsModel;
import net.java.truevfs.kernel.spec.cio.*;

/**
 * A mediator for the instrumentation of the TrueVFS Kernel with JMX.
 * 
 * @author Christian Schlichtherle
 */
@ThreadSafe
abstract class JmxMediator
extends net.java.truevfs.comp.jmx.JmxMediator<JmxMediator> {

    private static final JmxMediator APP_IO = new JmxIoMediator("Application I/O");
    private static final JmxMediator BUFFER_IO = new JmxIoMediator("Buffer I/O");
    private static final JmxMediator KERNEL_IO = new JmxIoMediator("Kernel I/O");
    private static final JmxMediator SYNC_OPS = new JmxSyncMediator("Sync Operations");

    private static JmxMediator[] MEDIATORS = {
        SYNC_OPS, APP_IO, KERNEL_IO, BUFFER_IO
    };

    static JmxMediator get() { return SYNC_OPS; }

    private final String subject;
    private final FsLogger logger = new FsLogger();

    JmxMediator(final String subject) {
        this.subject = Objects.requireNonNull(subject);
        assert null != logger;
    }

    String getSubject() { return subject; }

    abstract JmxStatistics newStats(int offset);

    private void startStats(int offset) { start(newStats(offset)); }

    void startStats(JmxColleague origin) { startStats(0); }

    final void startAllStats(JmxColleague origin) {
        for (JmxMediator mediator : MEDIATORS) mediator.startStats(origin);
    }

    void rotateStats(JmxColleague origin) { startStats(logger.rotate()); }

    final void rotateAllStats(JmxColleague origin) {
        for (JmxMediator mediator : MEDIATORS) mediator.rotateStats(origin);
    }

    final void logRead(long nanos, int bytes) { logger.logRead(nanos, bytes); }

    final void logWrite(long nanos, int bytes) { logger.logWrite(nanos, bytes); }

    final void logSync(long nanos) { logger.logSync(nanos); }

    final FsStatistics getStats(int offset) { return logger.stats(offset); }

    final String formatOffset(int offset) { return logger.format(offset); }

    @Override
    public final FsManager instrument(FsManager object) {
        return start(new JmxManager(SYNC_OPS, object)); // switch mediator!
    }

    @Override
    public final IoBufferPool instrument(IoBufferPool object) {
        return new InstrumentingBufferPool<>(BUFFER_IO, object); // switch mediator!
    }

    @Override
    public FsMetaDriver instrument(
            InstrumentingManager<JmxMediator> origin,
            FsMetaDriver object) {
        return new InstrumentingMetaDriver<>(this, object);
    }

    @Override
    public final FsController instrument(
            InstrumentingManager<JmxMediator> origin,
            FsController object) {
        return new InstrumentingController<>(APP_IO, object); // switch mediator!
    }

    @Override
    public final IoBuffer instrument(
            InstrumentingBufferPool<JmxMediator> origin,
            IoBuffer object) {
        return start(new JmxBuffer<>(this, object));
    }

    @Override
    public final FsModel instrument(
            InstrumentingMetaDriver<JmxMediator> origin,
            FsModel object) {
        return start(new JmxModel<>(this, object));
    }

    @Override
    public final FsController instrument(
            InstrumentingMetaDriver<JmxMediator> origin,
            FsController object) {
        return new InstrumentingController<>(KERNEL_IO, object); // switch mediator!
    }

    @Override
    public final <E extends Entry> InputSocket<E> instrument(
            InstrumentingController<JmxMediator> origin,
            InputSocket<E> object) {
        return new InstrumentingInputSocket<>(this, object);
    }

    @Override
    public final <E extends Entry> OutputSocket<E> instrument(
            InstrumentingController<JmxMediator> origin,
            OutputSocket<E> object) {
        return new InstrumentingOutputSocket<>(this, object);
    }

    @Override
    public final <B extends IoBuffer> InputSocket<B> instrument(
            InstrumentingBuffer<JmxMediator> origin,
            InputSocket<B> object) {
        return new InstrumentingInputSocket<>(this, object);
    }

    @Override
    public final <B extends IoBuffer> OutputSocket<B> instrument(
            InstrumentingBuffer<JmxMediator> origin,
            OutputSocket<B> object) {
        return new InstrumentingOutputSocket<>(this, object);
    }

    @Override
    public final <E extends Entry> InputStream instrument(
            InstrumentingInputSocket<JmxMediator, E> origin,
            InputStream object) {
        return start(new JmxInputStream(this, object));
    }

    @Override
    public final <E extends Entry> SeekableByteChannel instrument(
            InstrumentingInputSocket<JmxMediator, E> origin,
            SeekableByteChannel object) {
        return start(new JmxSeekableChannel(this, object));
    }

    @Override
    public final <E extends Entry> OutputStream instrument(
            InstrumentingOutputSocket<JmxMediator, E> origin,
            OutputStream object) {
        return start(new JmxOutputStream(this, object));
    }

    @Override
    public final <E extends Entry> SeekableByteChannel instrument(
            InstrumentingOutputSocket<JmxMediator, E> origin,
            SeekableByteChannel object) {
        return start(new JmxSeekableChannel(this, object));
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
}
