/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.io;

import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;

/**
 * A source which can get used only once to obtain a given input stream or
 * seekable byte channel.
 *
 * @see    OneTimeSink
 * @author Christian Schlichtherle
 */
public final class OneTimeSource
extends OneTimeFoundry<InputStream, SeekableByteChannel>
implements Source {

    public OneTimeSource(InputStream in) {
        super(in);
    }

    public OneTimeSource(SeekableByteChannel channel) {
        super(channel);
    }
}
