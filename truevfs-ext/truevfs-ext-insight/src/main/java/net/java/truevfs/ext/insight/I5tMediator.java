/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.insight;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.Objects;
import javax.annotation.concurrent.ThreadSafe;
import net.java.truevfs.comp.inst.*;
import net.java.truevfs.comp.jmx.JmxBuffer;
import net.java.truevfs.comp.jmx.JmxColleague;
import net.java.truevfs.comp.jmx.JmxMediator;
import net.java.truevfs.comp.jmx.JmxModel;
import net.java.truevfs.ext.insight.stats.FsLogger;
import net.java.truevfs.ext.insight.stats.FsStatistics;
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
abstract class I5tMediator extends JmxMediator<I5tMediator> {

    private static final I5tMediator APP_IO = new I5tIoMediator("Application I/O");
    private static final I5tMediator BUFFER_IO = new I5tIoMediator("Buffer I/O");
    private static final I5tMediator KERNEL_IO = new I5tIoMediator("Kernel I/O");
    private static final I5tMediator SYNC_OPS = new I5tSyncMediator("Sync Operations");

    private static I5tMediator[] MEDIATORS = {
        SYNC_OPS, APP_IO, KERNEL_IO, BUFFER_IO
    };

    static I5tMediator get() { return SYNC_OPS; }

    private final String subject;
    private final FsLogger logger = new FsLogger();

    I5tMediator(final String subject) {
        this.subject = Objects.requireNonNull(subject);
    }

    String getSubject() { return subject; }

    abstract I5tStatistics newStats(int offset);

    private void startStats(int offset) { start(newStats(offset)); }

    void startStats(JmxColleague origin) { startStats(0); }

    final void startAllStats(JmxColleague origin) {
        for (I5tMediator mediator : MEDIATORS) mediator.startStats(origin);
    }

    void rotateStats(JmxColleague origin) { startStats(logger.rotate()); }

    final void rotateAllStats(JmxColleague origin) {
        for (I5tMediator mediator : MEDIATORS) mediator.rotateStats(origin);
    }

    final void logRead(long nanos, int bytes) { logger.logRead(nanos, bytes); }

    final void logWrite(long nanos, int bytes) { logger.logWrite(nanos, bytes); }

    final void logSync(long nanos) { logger.logSync(nanos); }

    final FsStatistics getStats(int offset) { return logger.stats(offset); }

    final String formatOffset(int offset) { return logger.format(offset); }

    @Override
    public final FsManager instrument(FsManager object) {
        return start(new I5tManager(SYNC_OPS, object)); // switch mediator!
    }

    @Override
    public final IoBufferPool instrument(IoBufferPool object) {
        return new InstrumentingBufferPool<>(BUFFER_IO, object); // switch mediator!
    }

    @Override
    public FsMetaDriver instrument(
            InstrumentingManager<I5tMediator> origin,
            FsMetaDriver object) {
        return new InstrumentingMetaDriver<>(this, object);
    }

    @Override
    public final FsController instrument(
            InstrumentingManager<I5tMediator> origin,
            FsController object) {
        return new InstrumentingController<>(APP_IO, object); // switch mediator!
    }

    @Override
    public final IoBuffer instrument(
            InstrumentingBufferPool<I5tMediator> origin,
            IoBuffer object) {
        return start(new JmxBuffer<>(this, object));
    }

    @Override
    public final FsModel instrument(
            InstrumentingMetaDriver<I5tMediator> origin,
            FsModel object) {
        return start(new JmxModel<>(this, object));
    }

    @Override
    public final FsController instrument(
            InstrumentingMetaDriver<I5tMediator> origin,
            FsController object) {
        return new InstrumentingController<>(KERNEL_IO, object); // switch mediator!
    }

    @Override
    public final <E extends Entry> InputSocket<E> instrument(
            InstrumentingController<I5tMediator> origin,
            InputSocket<E> object) {
        return new InstrumentingInputSocket<>(this, object);
    }

    @Override
    public final <E extends Entry> OutputSocket<E> instrument(
            InstrumentingController<I5tMediator> origin,
            OutputSocket<E> object) {
        return new InstrumentingOutputSocket<>(this, object);
    }

    @Override
    public final <B extends IoBuffer> InputSocket<B> instrument(
            InstrumentingBuffer<I5tMediator> origin,
            InputSocket<B> object) {
        return new InstrumentingInputSocket<>(this, object);
    }

    @Override
    public final <B extends IoBuffer> OutputSocket<B> instrument(
            InstrumentingBuffer<I5tMediator> origin,
            OutputSocket<B> object) {
        return new InstrumentingOutputSocket<>(this, object);
    }

    @Override
    public final <E extends Entry> InputStream instrument(
            InstrumentingInputSocket<I5tMediator, E> origin,
            InputStream object) {
        return start(new I5tInputStream(this, object));
    }

    @Override
    public final <E extends Entry> SeekableByteChannel instrument(
            InstrumentingInputSocket<I5tMediator, E> origin,
            SeekableByteChannel object) {
        return start(new I5tSeekableChannel(this, object));
    }

    @Override
    public final <E extends Entry> OutputStream instrument(
            InstrumentingOutputSocket<I5tMediator, E> origin,
            OutputStream object) {
        return start(new I5tOutputStream(this, object));
    }

    @Override
    public final <E extends Entry> SeekableByteChannel instrument(
            InstrumentingOutputSocket<I5tMediator, E> origin,
            SeekableByteChannel object) {
        return start(new I5tSeekableChannel(this, object));
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
