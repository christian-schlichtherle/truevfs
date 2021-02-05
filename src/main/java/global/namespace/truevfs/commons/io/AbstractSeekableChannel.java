/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.commons.io;

import java.nio.channels.ClosedChannelException;
import java.nio.channels.SeekableByteChannel;

/**
 * An abstract seekable byte channel.
 * 
 * @author Christian Schlichtherle
 */
public abstract class AbstractSeekableChannel implements SeekableByteChannel {

    /**
     * Throws a {@link ClosedChannelException} iff {@link #isOpen()} returns
     * {@code false}.
     *
     * @throws ClosedChannelException iff {@link #isOpen()} returns
     *         {@code false}.
     */
    protected final void checkOpen() throws ClosedChannelException {
        if (!isOpen()) throw new ClosedChannelException();
    }
}
