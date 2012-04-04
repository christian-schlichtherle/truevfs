/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.sbc;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author Christian Schlichtherle
 */
public final class BufferedReadOnlyChannelIT extends SeekableByteChannelTestSuite {

    @Override
    protected SeekableByteChannel newChannel(Path path) throws IOException {
        return new BufferedReadOnlyChannel(Files.newByteChannel(path));
    }
}
