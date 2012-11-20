/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec;

import net.java.truecommons.io.Sink;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import net.java.truecommons.cio.Entry;
import net.java.truecommons.cio.OutputSocket;
import net.java.truecommons.shed.BitField;

/**
 * An adapter from an output socket to a sink with access options.
 * 
 * @author Christian Schlichtherle
 */
/* class FsOutputSocketSink
 * (val options: BitField[FsAccessOption], val socket: OutputSocket[_ <: Entry]) {
 *   def this(sink: FsOutputSocketSink) = this(sink.options, sink.socket)
 *   def stream() = socket stream null
 *   def channel() = socket channel null
 * }
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
        return getSocket().stream(null);
    }

    @Override
    public SeekableByteChannel channel() throws IOException {
        return getSocket().channel(null);
    }
}
