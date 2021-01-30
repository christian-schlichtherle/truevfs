/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.cio;

import net.java.truecommons.io.Sink;
import net.java.truecommons.io.Source;
import net.java.truecommons.io.Streams;

import javax.annotation.concurrent.Immutable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;

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
     * @param input an input socket for the input target.
     * @param output an output socket for the output target.
     */
    public static void copy(InputSocket<?> input, OutputSocket<?> output)
    throws IOException {
        Streams.copy(   new InputAdapter(input, output),
                        new OutputAdapter(output, input));
    }

    private static class InputAdapter implements Source {

        final InputSocket<? extends Entry> input;
        final OutputSocket<? extends Entry> output;

        InputAdapter(
                final InputSocket<? extends Entry> input,
                final OutputSocket<? extends Entry> output) {
            this.input = input;
            this.output = output;
        }

        @Override
        public InputStream stream() throws IOException {
            return input.stream(output);
        }

        @Override
        public SeekableByteChannel channel() throws IOException {
            return input.channel(output);
        }
    } // InputAdapter

    private static class OutputAdapter implements Sink {

        final OutputSocket<? extends Entry> output;
        final InputSocket<? extends Entry> input;

        OutputAdapter(
                final OutputSocket<? extends Entry> output,
                final InputSocket<? extends Entry> input) {
            this.output = output;
            this.input = input;
        }

        @Override
        public OutputStream stream() throws IOException {
            return output.stream(input);
        }

        @Override
        public SeekableByteChannel channel() throws IOException {
            return output.channel(input);
        }
    } // OutputAdapter
}
