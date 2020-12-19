package net.java.truevfs.ext.logging;

import net.java.truecommons.cio.Entry;
import net.java.truecommons.cio.IoSocket;
import net.java.truecommons.io.ReadOnlyChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.SeekableByteChannel;

final class LogInputChannel extends ReadOnlyChannel implements LogCloseable {

    private static final Logger logger = LoggerFactory.getLogger(LogInputChannel.class);

    private final IoSocket<? extends Entry> context;

    LogInputChannel(final IoSocket<? extends Entry> context, final SeekableByteChannel channel) {
        super(channel);
        this.context = context;
        opening();
    }

    @Override
    public IoSocket<? extends Entry> context() {
        return context;
    }

    @Override
    public Logger logger() {
        return logger;
    }
}
