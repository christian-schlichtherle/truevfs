/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;
import net.truevfs.kernel.cio.Entry;
import net.truevfs.kernel.cio.InputSocket;
import net.truevfs.kernel.io.Source;
import net.truevfs.kernel.util.BitField;

/**
 * An adapter from an input socket to a source with access options.
 * 
 * @author Christian Schlichtherle
 */
/* class FsInputSocketSource
 * (val options: BitField[FsAccessOption], val socket: InputSocket[_ <: Entry]) {
 *   def this(sink: FsInputSocketSource) = this(sink.options, sink.socket)
 *   def stream() = socket stream null
 *   def channel() = socket channel null
 * }
 */
public class FsInputSocketSource implements Source {
    private final BitField<FsAccessOption> options;
    private final InputSocket<? extends Entry> socket;

    public FsInputSocketSource(
            final BitField<FsAccessOption> options,
            final InputSocket<? extends Entry> socket) {
        this.options = options;
        this.socket = socket;
    }

    public FsInputSocketSource(final FsInputSocketSource source) {
        this.options = source.getOptions();
        this.socket = source.getSocket();
    }

    public BitField<FsAccessOption> getOptions() {
        return options;
    }

    public InputSocket<? extends Entry> getSocket() {
        return socket;
    }

    @Override
    public InputStream stream() throws IOException {
        return getSocket().stream(null);
    }

    @Override
    public SeekableByteChannel channel() throws IOException {
        return getSocket().channel(null);
    }
}
