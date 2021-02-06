/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.cio;

import global.namespace.truevfs.comp.io.Sink;
import global.namespace.truevfs.comp.io.Source;
import global.namespace.truevfs.comp.io.Streams;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.Optional;

/**
 * Provides utility methods for {@link IoSocket}s.
 *
 * @author Christian Schlichtherle
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public final class IoSockets {

    private IoSockets() {
    }

    /**
     * Copies an input stream {@link InputSocket#stream created} by the given {@code input} socket to an output stream
     * {@link OutputSocket#stream created} by the given {@code output} socket.
     * <p>
     * This is a high performance implementation which uses a pooled daemon thread to fill a FIFO of pooled buffers
     * which is concurrently flushed by the current thread.
     *
     * @param input  an input socket for the input target.
     * @param output an output socket for the output target.
     */
    public static void copy(InputSocket<?> input, OutputSocket<?> output) throws IOException {

        class Input implements Source {

            @Override
            public InputStream stream() throws IOException {
                return input.stream(Optional.of(output));
            }

            @Override
            public SeekableByteChannel channel() throws IOException {
                return input.channel(Optional.of(output));
            }
        }

        class Output implements Sink {

            @Override
            public OutputStream stream() throws IOException {
                return output.stream(Optional.of(input));
            }

            @Override
            public SeekableByteChannel channel() throws IOException {
                return output.channel(Optional.of(input));
            }
        }

        Streams.copy(new Input(), new Output());
    }
}
