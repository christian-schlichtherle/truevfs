/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.ext.insight;

import global.namespace.truevfs.commons.cio.*;
import global.namespace.truevfs.commons.inst.*;
import global.namespace.truevfs.commons.jmx.JmxBuffer;
import global.namespace.truevfs.commons.jmx.JmxComponent;
import global.namespace.truevfs.commons.jmx.JmxMediator;
import global.namespace.truevfs.commons.jmx.JmxModel;
import global.namespace.truevfs.ext.insight.stats.FsLogger;
import global.namespace.truevfs.ext.insight.stats.FsStats;
import global.namespace.truevfs.kernel.api.FsCompositeDriver;
import global.namespace.truevfs.kernel.api.FsController;
import global.namespace.truevfs.kernel.api.FsManager;
import global.namespace.truevfs.kernel.api.FsModel;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;

import static global.namespace.truevfs.ext.insight.I5tMediators.*;
import static java.util.Locale.ENGLISH;

/**
 * A base mediator for the instrumentation of the TrueVFS Kernel with JMX.
 *
 * @author Christian Schlichtherle
 */
abstract class I5tMediator extends JmxMediator<I5tMediator> {

    private final FsLogger logger = new FsLogger();

    private final String subject;

    I5tMediator(final String subject) {
        assert null != subject;
        this.subject = subject;
    }

    final String getSubject() {
        return subject;
    }

    abstract I5tStatsController newController(int offset);

    private void activateStats(int offset) {
        activate(newController(offset));
    }

    final void activateStats(JmxComponent origin) {
        activateStats(0);
    }

    final void activateAllStats(final JmxComponent origin) {
        for (I5tMediator mediator : mediators) {
            mediator.activateStats(origin);
        }
    }

    void rotateStats(JmxComponent origin) {
        activateStats(logger.rotate());
    }

    final void rotateAllStats(JmxComponent origin) {
        for (I5tMediator mediator : mediators) {
            mediator.rotateStats(origin);
        }
    }

    final void logRead(long nanos, int bytes) {
        logger.logRead(nanos, bytes);
    }

    final void logWrite(long nanos, int bytes) {
        logger.logWrite(nanos, bytes);
    }

    final void logSync(long nanos) {
        logger.logSync(nanos);
    }

    final FsStats stats(int offset) {
        return logger.stats(offset);
    }

    final String formatOffset(int offset) {
        return logger.format(offset);
    }

    @Override
    public final String toString() {
        return String.format(ENGLISH, "%s[subject=%s]", getClass().getName(), subject);
    }

    @Override
    public final FsManager instrument(FsManager subject) {
        return activate(new I5tManager(syncOperationsMediator, subject));
    }

    @Override
    public final IoBufferPool instrument(IoBufferPool subject) {
        return new InstrumentingBufferPool<>(bufferIoMediator, subject);
    }

    @Override
    public final FsCompositeDriver instrument(InstrumentingManager<I5tMediator> context, FsCompositeDriver subject) {
        return new InstrumentingCompositeDriver<>(this, subject);
    }

    @Override
    public final FsController instrument(InstrumentingManager<I5tMediator> context, FsController subject) {
        return new InstrumentingController<>(applicationIoMediator, subject);
    }

    @Override
    public final IoBuffer instrument(InstrumentingBufferPool<I5tMediator> context, IoBuffer subject) {
        return activate(new JmxBuffer<>(this, subject));
    }

    @Override
    public final FsModel instrument(InstrumentingCompositeDriver<I5tMediator> context, FsModel subject) {
        return activate(new JmxModel<>(this, subject));
    }

    @Override
    public final FsController instrument(InstrumentingCompositeDriver<I5tMediator> context, FsController subject) {
        return new InstrumentingController<I5tMediator>(kernelIoMediator, subject);
    }

    @Override
    public final <E extends Entry> InputSocket<E> instrument(InstrumentingController<I5tMediator> context, InputSocket<E> subject) {
        return new InstrumentingInputSocket<>(this, subject);
    }

    @Override
    public final <E extends Entry> OutputSocket<E> instrument(InstrumentingController<I5tMediator> context, OutputSocket<E> subject) {
        return new InstrumentingOutputSocket<>(this, subject);
    }

    @Override
    public final <B extends IoBuffer> InputSocket<B> instrument(InstrumentingBuffer<I5tMediator> context, InputSocket<B> subject) {
        return new InstrumentingInputSocket<>(this, subject);
    }

    @Override
    public final <B extends IoBuffer> OutputSocket<B> instrument(InstrumentingBuffer<I5tMediator> context, OutputSocket<B> subject) {
        return new InstrumentingOutputSocket<>(this, subject);
    }

    @Override
    public final <E extends Entry> InputStream instrument(InstrumentingInputSocket<I5tMediator, E> context, InputStream subject) {
        return activate(new I5tInputStream(this, subject));
    }

    @Override
    public final <E extends Entry> SeekableByteChannel instrument(InstrumentingInputSocket<I5tMediator, E> context, SeekableByteChannel subject) {
        return activate(new I5tSeekableChannel(this, subject));
    }

    @Override
    public final <E extends Entry> OutputStream instrument(InstrumentingOutputSocket<I5tMediator, E> context, OutputStream subject) {
        return activate(new I5tOutputStream(this, subject));
    }

    @Override
    public final <E extends Entry> SeekableByteChannel instrument(InstrumentingOutputSocket<I5tMediator, E> context, SeekableByteChannel subject) {
        return activate(new I5tSeekableChannel(this, subject));
    }
}
