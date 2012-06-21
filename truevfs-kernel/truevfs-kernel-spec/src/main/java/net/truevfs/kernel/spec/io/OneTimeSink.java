/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.spec.io;

import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;

/**
 * A sink which can get used only once to obtain a given output stream or
 * seekable byte channel.
 * 
 * @see    OneTimeSource
 * @author Christian Schlichtherle
 */
public final class OneTimeSink
extends OneTimeFoundry<OutputStream, SeekableByteChannel>
implements Sink {

    public OneTimeSink(OutputStream out) {
        super(out);
    }

    public OneTimeSink(SeekableByteChannel channel) {
        super(channel);
    }
}
