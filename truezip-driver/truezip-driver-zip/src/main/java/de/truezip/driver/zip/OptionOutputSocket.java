/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip;

import de.truezip.kernel.cio.Entry;
import de.truezip.kernel.option.FsAccessOption;
import de.truezip.kernel.cio.DecoratingOutputSocket;
import de.truezip.kernel.cio.OutputSocket;
import de.truezip.kernel.util.BitField;
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
    final BitField<FsAccessOption> options;

    public OptionOutputSocket(
            final OutputSocket<?> output,
            final BitField<FsAccessOption> options) {
        super(output);
        this.options = options;
    }

    public BitField<FsAccessOption> getOptions() {
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