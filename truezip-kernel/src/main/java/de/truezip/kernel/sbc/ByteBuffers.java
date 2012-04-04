/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.sbc;

import java.nio.ByteBuffer;

/**
 * Utility methods for {@link ByteBuffer}s.
 * 
 * @author Christian Schlichtherle
 */
final class ByteBuffers {

    /** Can't touch this - hammer time! */
    private ByteBuffers() { }

    public static int copy(final ByteBuffer src, final ByteBuffer dst) {
        final int available = src.remaining();
        if (0 >= available)
            return -1;
        int remaining = dst.remaining();
        if (remaining > available)
            remaining = available;
        final int limit;
        if (available > remaining) {
            limit = src.limit();
            src.limit(src.position() + remaining);
        } else {
            limit = -1;
        }
        try {
            dst.put(src);
        } finally {
            if (0 <= limit)
                src.limit(limit);
        }
        return remaining;
    }
}
