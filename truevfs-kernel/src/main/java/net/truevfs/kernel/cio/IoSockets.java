/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.cio;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.concurrent.Immutable;
import net.truevfs.kernel.io.InputException;
import net.truevfs.kernel.io.Sink;
import net.truevfs.kernel.io.Source;
import net.truevfs.kernel.io.Streams;

/**
 * Provides utility methods for {@link IoSocket}s.
 *
 * @author Christian Schlichtherle
 */
@Immutable
public final class IoSockets {

    /** Can't touch this - hammer time! */
    private IoSockets() { }

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
        Streams.copy(   new InputSocketAdapter(input),
                        new OutputSocketAdapter(output.connect(input)));
        // Disconnect for subsequent use, if any.
        input.connect(null); // or output.connect(null)
    }

    private static class InputSocketAdapter implements Source {
        final InputSocket<? extends Entry> input;

        InputSocketAdapter(final InputSocket<? extends Entry> input) {
            this.input = input;//Objects.requireNonNull(input);
        }

        @Override
        public InputStream stream() throws IOException {
            return input.stream();
        }

        @Override
        public SeekableByteChannel channel() throws IOException {
            return input.channel();
        }
    } // InputSocketAdapter

    private static class OutputSocketAdapter implements Sink {
        final OutputSocket<? extends Entry> output;

        OutputSocketAdapter(final OutputSocket<? extends Entry> output) {
            this.output = output;//Objects.requireNonNull(output);
        }

        @Override
        public OutputStream stream() throws IOException {
            return output.stream();
        }

        @Override
        public SeekableByteChannel channel() throws IOException {
            return output.channel();
        }
    } // OutputSocketAdapter
}
