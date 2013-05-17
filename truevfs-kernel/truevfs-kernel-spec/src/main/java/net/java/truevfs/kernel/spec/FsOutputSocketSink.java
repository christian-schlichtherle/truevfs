/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec;

import java.io.*;
import java.nio.channels.SeekableByteChannel;
import java.util.Objects;
import net.java.truecommons.cio.*;
import net.java.truecommons.io.Sink;
import net.java.truecommons.shed.BitField;

/**
 * An adapter from an output socket to a sink with access options.
 *
 * @author Christian Schlichtherle
 */
public class FsOutputSocketSink implements Sink {

    private final BitField<FsAccessOption> options;
    private final OutputSocket<? extends Entry> socket;

    public FsOutputSocketSink(
            final BitField<FsAccessOption> options,
            final OutputSocket<? extends Entry> socket) {
        this.options = Objects.requireNonNull(options);
        this.socket = Objects.requireNonNull(socket);
    }

    public FsOutputSocketSink(final FsOutputSocketSink sink) {
        this.options = sink.getOptions();
        this.socket = sink.getSocket();
    }

    public BitField<FsAccessOption> getOptions() {
        return options;
    }

    public OutputSocket<? extends Entry> getSocket() {
        return socket;
    }

    @Override
    public OutputStream stream() throws IOException {
        return getSocket().stream(null);
    }

    @Override
    public SeekableByteChannel channel() throws IOException {
        return getSocket().channel(null);
    }
}
