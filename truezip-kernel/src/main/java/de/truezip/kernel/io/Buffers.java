/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.io;

import java.nio.Buffer;
import java.nio.ByteBuffer;

/**
 * Utility methods for {@link Buffer}s.
 * 
 * @author Christian Schlichtherle
 */
public final class Buffers {

    /** Can't touch this - hammer time! */
    private Buffers() { }

    public static int copy(final ByteBuffer src, final ByteBuffer dst) {
        int remaining = dst.remaining();
        if (remaining <= 0)
            return 0;
        final int available = src.remaining();
        if (available <= 0)
            return -1;
        final int srcLimit;
        if (available > remaining) {
            srcLimit = src.limit();
            src.limit(src.position() + remaining);
        } else {
            srcLimit = -1;
            remaining = available;
        }
        try {
            dst.put(src);
        } finally {
            if (srcLimit >= 0)
                src.limit(srcLimit);
        }
        return remaining;
    }
}
