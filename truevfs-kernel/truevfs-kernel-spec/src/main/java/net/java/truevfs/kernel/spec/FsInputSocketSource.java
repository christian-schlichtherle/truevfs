/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec;

import java.io.*;
import java.nio.channels.SeekableByteChannel;
import java.util.Objects;
import net.java.truecommons.cio.*;
import net.java.truecommons.io.Source;
import net.java.truecommons.shed.BitField;

/**
 * An adapter from an input socket to a source with access options.
 *
 * @author Christian Schlichtherle
 */
public class FsInputSocketSource implements Source {

    private final BitField<FsAccessOption> options;
    private final InputSocket<? extends Entry> socket;

    public FsInputSocketSource(
            final BitField<FsAccessOption> options,
            final InputSocket<? extends Entry> socket) {
        this.options = Objects.requireNonNull(options);
        this.socket = Objects.requireNonNull(socket);
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
