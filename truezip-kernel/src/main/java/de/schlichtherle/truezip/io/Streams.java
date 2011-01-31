/*
 * Copyright (C) 2007-2011 Schlichtherle IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.schlichtherle.truezip.io;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides static utility methods for {@link InputStream}s and
 * {@link OutputStream}s.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
public final class Streams {

    /** You cannot instantiate this class. */
    private Streams() {
    }

    private static final ExecutorService executor
            = Executors.newCachedThreadPool(new InputStreamReaderThreadFactory());

    /** A factory for input stream reader threads. */
    private static class InputStreamReaderThreadFactory
    implements ThreadFactory {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "TrueZIP InputStream Reader");
            t.setDaemon(true);
            return t;
        }
    }

    /**
     * Copies the data from the given input stream to the given output stream
     * and <em>always</em> closes <em>both</em> streams - even if an exception
     * occurs.
     * <p>
     * This is a high performance implementation which uses a pooled background
     * thread to fill a FIFO of data buffers which is concurrently flushed by
     * the current thread.
     *
     * @param  in the input stream.
     * @param  out the output stream.
     * @throws InputException if copying the data fails because of an
     *         {@code IOException} in the <em>input</em> stream.
     * @throws IOException if copying the data fails because of an
     *         {@code IOException} in the <em>output</em> stream.
     */
    public static void copy(final InputStream in,
                            final OutputStream out)
    throws IOException {
        try {
            Streams.cat(in, out);
        } finally {
            try {
                in.close();
            } catch (IOException ex) {
                throw new InputException(ex);
            } finally {
                out.close();
            }
        }
    }

    /**
     * Copies the data from the given input stream to the given output stream
     * <em>without</em> closing them.
     * The name of this method is inspired by the Unix command line utility
     * {@code cat} because you could use it to con<i>cat</i>enate the contents
     * of multiple streams.
     * <p>
     * This is a high performance implementation which uses a pooled background
     * thread to fill a FIFO of data buffers which is concurrently flushed by
     * the current thread.
     *
     * @param  in the input stream.
     * @param  out the output stream.
     * @throws InputException if copying the data fails because of an
     *         {@code IOException} in the <em>input</em> stream.
     * @throws IOException if copying the data fails because of an
     *         {@code IOException} in the <em>output</em> stream.
     */
    public static void cat(final InputStream in, final OutputStream out)
    throws IOException {
        if (in == null || out == null)
            throw new NullPointerException();

        // Note that we do not use PipedInput/OutputStream because these
        // classes are slooowww. This is partially because they are using
        // Object.wait()/notify() in a suboptimal way and partially because
        // they copy data to and from an additional buffer byte array, which
        // is redundant if the data to be transferred is already held in
        // another byte array.
        // As an implication of the latter reason, although the idea of
        // adopting the pipe concept to threads looks tempting it is actually
        // bad design: Pipes are a good means of interprocess communication,
        // where processes cannot access each others data directly without
        // using an external data structure like the pipe as a commonly shared
        // FIFO buffer.
        // However, threads are different: They share the same memory and thus
        // we can use much more elaborated algorithms for data transfer.

        // Finally, in this case we will simply cycle through an array of
        // byte buffers, where an additionally created reader executor will fill
        // the buffers with data from the input and the current executor will
        // flush the filled buffers to the output.

        final Buffer[] buffers = allocateBuffers();

        /*
         * The task that cycles through the buffers in order to fill them
         * with input.
         */
        class Reader implements Runnable {
            /** The index of the next buffer to be written. */
            int off;

            /** The number of buffers filled with data to be written. */
            int len;

            /** The IOException that happened in this task, if any. */
            volatile InputException exception;

            @Override
            public void run() {
                // Cache some data for better performance.
                final InputStream _in = in;
                final Buffer[] _buffers = buffers;
                final int _buffersLen = buffers.length;

                // The writer executor interrupts this executor to signal
                // that it cannot handle more input because there has been
                // an IOException during writing.
                // We stop processing in this case.
                int read;
                do {
                    // Wait until a buffer is available.
                    final Buffer buffer;
                    synchronized (Reader.this) {
                        while (len >= _buffersLen) {
                            try {
                                wait();
                            } catch (InterruptedException interrupted) {
                                return; // stop reading.
                            }
                        }
                        buffer = _buffers[(off + len) % _buffersLen];
                    }

                    // Fill buffer until end of file or buffer.
                    // This should normally complete in one loop cycle, but
                    // we do not depend on this as it would be a violation
                    // of InputStream's contract.
                    final byte[] buf = buffer.buf;
                    try {
                        read = _in.read(buf, 0, buf.length);
                    } catch (IOException ex) {
                        exception = new InputException(ex);
                        read = -1;
                    }
                    /*if (Thread.interrupted())
                    read = -1; // throws away buf - OK in this context*/
                    buffer.read = read;

                    // Advance head and notify writer.
                    synchronized (Reader.this) {
                        len++;
                        notify(); // only the writer could be waiting now!
                    }
                } while (read != -1);
            }
        } // class Reader

        try {
            final Reader reader = new Reader();
            final Future<?> task = executor.submit(reader);

            // Cache some data for better performance.
            final int buffersLen = buffers.length;

            int write;
            while (true) {
                // Wait until a buffer is available.
                final int off;
                final Buffer buffer;
                synchronized (reader) {
                    while (reader.len <= 0) {
                        try {
                            reader.wait();
                        } catch (InterruptedException ignored) {
                        }
                    }
                    off = reader.off;
                    buffer = buffers[off];
                }

                // Stop on last buffer.
                write = buffer.read;
                if (write == -1)
                    break; // reader has terminated because of EOF or exception

                // Process buffer.
                final byte[] buf = buffer.buf;
                try {
                    out.write(buf, 0, write);
                } catch (IOException ex) {
                    // Cancel reader thread synchronously.
                    // Cancellation of the reader thread is required
                    // so that a re-entry to the cat(...) method by the same
                    // thread cannot not reuse the same shared buffers that
                    // an unfinished reader thread of a previous call is
                    // still using.
                    task.cancel(true);
                    while (true) {
                        try {
                            task.get();
                            break;
                        } catch (CancellationException ex2) {
                            break;
                        } catch (ExecutionException ex2) {
                            throw new AssertionError(ex2);
                        } catch (InterruptedException ex2) {
                            Logger  .getLogger(Streams.class.getName())
                                    .log(Level.INFO, ex2.getLocalizedMessage(), ex2);
                        }
                    }
                    throw ex;
                }

                // Advance tail and notify reader.
                synchronized (reader) {
                    reader.off = (off + 1) % buffersLen;
                    reader.len--;
                    reader.notify(); // only the reader could be waiting now!
                }
            }

            if (reader.exception != null)
                throw reader.exception;
        } finally {
            releaseBuffers(buffers);
        }
    }

    private static Buffer[] allocateBuffers() {
        synchronized (Buffer.list) {
            Buffer[] buffers;
            for (Iterator<Reference<Buffer[]>> i = Buffer.list.iterator(); i.hasNext(); ) {
                buffers = i.next().get();
                i.remove();
                if (buffers != null)
                    return buffers;
                }
            }

        // A minimum of two buffers is required.
        // The actual number is optimized to compensate for oscillating
        // I/O bandwidths like e.g. with network shares.
        final Buffer[] buffers = new Buffer[4];
        for (int i = buffers.length; --i >= 0; )
            buffers[i] = new Buffer();
        return buffers;
    }

    private static void releaseBuffers(Buffer[] buffers) {
        synchronized (Buffer.list) {
            Buffer.list.add(new SoftReference<Buffer[]>(buffers));
        }
    }

    /** A buffer for I/O. */
    private static class Buffer {
        /**
         * Each entry in this list holds a soft reference to an array
         * initialized with instances of this class.
         */
        static final List<Reference<Buffer[]>> list = new LinkedList<Reference<Buffer[]>>();

        /** The byte buffer used for asynchronous reading and writing. */
        byte[] buf = new byte[64 * 1024];

        /** The actual number of bytes read into the buffer. */
        int read;
    }
}
