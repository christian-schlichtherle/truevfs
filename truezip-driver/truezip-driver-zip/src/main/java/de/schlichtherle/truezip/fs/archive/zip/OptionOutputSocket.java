/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.archive.zip;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.fs.option.FsOutputOption;
import de.schlichtherle.truezip.entry.DecoratingOutputSocket;
import de.schlichtherle.truezip.entry.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;

/**
 * An output socket which provides a property for its output options.
 * 
 * @author  Christian Schlichtherle
 */
public final class OptionOutputSocket
extends DecoratingOutputSocket<Entry> {
    final BitField<FsOutputOption> options;

    public OptionOutputSocket(
            final OutputSocket<?> output,
            final BitField<FsOutputOption> options) {
        super(output);
        this.options = options;
    }

    public BitField<FsOutputOption> getOptions() {
        return options;
    }

    @Override
    public Entry getLocalTarget() throws IOException {
        return getBoundDelegate().getLocalTarget();
    }

    @Override
    public SeekableByteChannel newSeekableByteChannel() throws IOException {
        return getBoundDelegate().newSeekableByteChannel();
    }

    @Override
    public OutputStream newOutputStream() throws IOException {
        return getBoundDelegate().newOutputStream();
    }
}