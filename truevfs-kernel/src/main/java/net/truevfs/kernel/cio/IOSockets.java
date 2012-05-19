/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.cio;

import net.truevfs.kernel.io.InputException;
import net.truevfs.kernel.io.Streams;
import java.io.IOException;
import javax.annotation.concurrent.Immutable;

/**
 * Provides utility methods for {@link IOSocket}s.
 *
 * @author Christian Schlichtherle
 */
@Immutable
public final class IOSockets {

    /** Can't touch this - hammer time! */
    private IOSockets() { }

    /**
     * Copies an input stream {@link InputSocket#stream created}
     * by the given {@code input} socket to an output stream
     * {@link OutputSocket#stream created} by the given {@code output}
     * socket.
     * <p>
     * This is a high performance implementation which uses a pooled daemon
     * thread to fill a FIFO of pooled buffers which is concurrently flushed by
     * the current thread.
     *
     * @param  input an input socket for the input target.
     * @param  output an output socket for the output target.
     * @throws InputException if copying the data fails because of an
     *         {@code IOException} thrown by the <em>input socket</em>.
     * @throws IOException if copying the data fails because of an
     *         {@code IOException} thrown by the <em>output socket</em>.
     */
    public static void copy(InputSocket<?> input, OutputSocket<?> output)
    throws InputException, IOException {
        // Call connect on output for early NPE check!
        Streams.copy(input, output.connect(input));
        // Disconnect for subsequent use, if any.
        input.connect(null); // or output.connect(null)
    }
}
