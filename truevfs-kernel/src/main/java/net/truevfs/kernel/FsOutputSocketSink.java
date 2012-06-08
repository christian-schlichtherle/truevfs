/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import net.truevfs.kernel.cio.Entry;
import net.truevfs.kernel.cio.OutputSocket;
import net.truevfs.kernel.io.Sink;
import net.truevfs.kernel.util.BitField;

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
        this.options = options;
        this.socket = socket;
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
        return getSocket().stream();
    }

    @Override
    public SeekableByteChannel channel() throws IOException {
        return getSocket().channel();
    }
}
