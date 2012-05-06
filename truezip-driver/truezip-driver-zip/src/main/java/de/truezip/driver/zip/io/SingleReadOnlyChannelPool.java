/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.io;

import de.truezip.kernel.util.Pool;
import java.io.IOException;
import java.nio.channels.SeekableByteChannel;

/**
 * A pool with a single read-only seekable byte channel.
 *
 * @author Christian Schlichtherle
 */
final class SingleReadOnlyChannelPool implements Pool<SeekableByteChannel, IOException> {
    final SeekableByteChannel sbc;

    SingleReadOnlyChannelPool(final SeekableByteChannel rof) {
        this.sbc = rof;
    }

    @Override
    public SeekableByteChannel allocate() {
        return sbc;
    }

    @Override
    public void release(SeekableByteChannel rof) {
        assert this.sbc == rof;
    }
}
