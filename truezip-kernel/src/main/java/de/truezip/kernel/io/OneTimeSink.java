/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.io;

import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;

/**
 * @see    OneTimeSource
 * @author Christian Schlichtherle
 */
public final class OneTimeSink
extends OneTimeResource<OutputStream>
implements Sink {

    public OneTimeSink(OutputStream out) {
        super(out);
    }

    public OneTimeSink(SeekableByteChannel channel) {
        super(channel);
    }
}
