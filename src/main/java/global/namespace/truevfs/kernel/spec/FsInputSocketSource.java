/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.kernel.spec;

import global.namespace.truevfs.comp.cio.Entry;
import global.namespace.truevfs.comp.cio.InputSocket;
import global.namespace.truevfs.comp.io.Source;
import global.namespace.truevfs.comp.shed.BitField;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.Objects;
import java.util.Optional;

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
        return getSocket().stream(Optional.empty());
    }

    @Override
    public SeekableByteChannel channel() throws IOException {
        return getSocket().channel(Optional.empty());
    }
}
