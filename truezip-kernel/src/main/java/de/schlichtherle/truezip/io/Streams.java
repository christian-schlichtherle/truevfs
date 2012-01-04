/*
 * Copyright (C) 2004-2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.io;

import de.schlichtherle.truezip.util.ThreadGroups;
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
import java.util.concurrent.*;
import net.jcip.annotations.ThreadSafe;

/**
 * Provides static utility methods for {@link InputStream}s and
 * {@link OutputStream}s.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
@ThreadSafe
public final class Streams {

    /**
     * The size of the FIFO used for exchanging I/O buffers between a reader
     * thread and a writer thread.
     * A minimum of two elements is required.
     * The actual number is optimized to compensate for oscillating I/O
     * bandwidths like e.g. with network shares.
     */
    private static final int FIFO_SIZE = 4;

    /**
     * The buffer size used for reading and writing.
     * Optimized for performance.
     */
    public static final int BUFFER_SIZE = 8 * 1024;

    private static final ExecutorService executor
            = Executors.newCachedThreadPool(new ReaderThreadFactory());
    
    /** You cannot instantiate this class. */
    private Streams() {
    }

    /**
     * Copies the data from the given input stream to the given output stream
     * and <em>always</em> closes <em>both</em> streams - even if an exception
     * occurs.
     * <p>
     * This is a high performance implementation which uses a pooled background
     * thread to fill a FIFO of pooled buffers which is concurrently flushed by
     * the current thread.
     * It performs best when used with <em>unbuffered</em> streams.
     *
     * @param  in the input stream.
     * @param  out the output stream.
     * @throws InputException if copying the data fails because of an
     *         {@code IOException} in the <em>input stream</em>.
     * @throws IOException if copying the data fails because of an
     *         {@code IOException} in the <em>output stream</em>.
     */
    public static void copy(final InputStream in, final OutputStream out)
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
     * This method calls {@link OutputStream#flush()} unless an
     * {@link IOException} occurs when writing to the output stream.
     * This hold true even if an {@link IOException} occurs when reading from
     * the input stream.
     * <p>
     * This is a high performance implementation which uses a pooled background
     * thread to fill a FIFO of pooled buffers which is concurrently flushed by
     * the current thread.
     * It performs best when used with <em>unbuffered</em> streams.
     * <p>
     * The name of this method is inspired by the Unix command line utility
     * {@code cat} because you could use it to con<i>cat</i>enate the contents
     * of multiple streams.
     *
     * @param  in the input stream.
     * @param  out the output stream.
     * @throws InputException if copying the data fails because of an
     *         {@code IOException} in the <em>input stream</em>.
     * @throws IOException if copying the data fails because of an
     *         {@code IOException} in the <em>output stream</em>.
     */
    public static void cat(final InputStream in, final OutputStream out)
    throws IOException {
        if (null == in || null == out)
            throw new NullPointerException();

        // Note that we do not use PipedInput/OutputStream because these
        // classes are slooow. This is partially because they are using
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

        // Finally, in this case we will use a FIFO to exchange byte buffers
        // between an additional reader thread and the current writer thread.
        // An additionally created reader thread will fill the buffers with
        // data from the input and the current thread will flush the filled
        // buffers to the output.
        // The FIFO is simply implemented as an array with an offset and a size
        // which is used like a ring buffer.

        final Buffer[] buffers = Buffer.allocate();

        /*
         * The task that cycles through the buffers in order to fill them
         * with input.
         */
        class ReaderTask implements Runnable {
            /** The index of the next buffer to be written. */
            int off;

            /** The number of buffers filled with data to be written. */
            int size;

            /** The IOException that happened in this task, if any. */
            volatile InputException exception;

            @Override
            public void run() {
                // Cache some fields for better performance.
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
                    synchronized (ReaderTask.this) {
                        while (size >= _buffersLen) {
                            try {
                                wait();
                            } catch (InterruptedException interrupted) {
                                // The writer thread wants us to stop reading.
                                return;
                            }
                        }
                        buffer = _buffers[(off + size) % _buffersLen];
                    }

                    // Fill buffer until end of file or buffer.
                    // This should normally complete in one loop cycle, but
                    // we do not depend on this as it would be a violation
                    // of InputStream's contract.
                    final byte[] buf = buffer.buf;
                    try {
                        read = _in.read(buf, 0, buf.length);
                    } catch (Throwable ex) {
                        exception = new InputException(ex);
                        read = -1;
                    }
                    /*if (Thread.interrupted())
                        read = -1; // throws away buf - OK in this context*/
                    buffer.read = read;

                    // Advance head and notify writer.
                    synchronized (ReaderTask.this) {
                        size++;
                        notify(); // only the writer could be waiting now!
                    }
                } while (read != -1);
            }
        } // ReaderTask

        boolean interrupted = false;
        try {
            final ReaderTask task = new ReaderTask();
            final Future<?> result = executor.submit(task);

            // Cache some data for better performance.
            final int buffersLen = buffers.length;

            int write;
            while (true) {
                // Wait until a buffer is available.
                final int off;
                final Buffer buffer;
                synchronized (task) {
                    while (0 >= task.size) {
                        try {
                            task.wait();
                        } catch (InterruptedException ex) {
                            interrupted = true;
                        }
                    }
                    off = task.off;
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
                    result.cancel(true);
                    while (true) {
                        try {
                            result.get();
                            break;
                        } catch (CancellationException ex2) {
                            break;
                        } catch (ExecutionException ex2) {
                            throw new AssertionError(ex2);
                        } catch (InterruptedException ex2) {
                            interrupted = true;
                        }
                    }
                    throw ex;
                }

                // Advance tail and notify reader.
                synchronized (task) {
                    task.off = (off + 1) % buffersLen;
                    task.size--;
                    task.notify(); // only the reader could be waiting now!
                }
            }
            out.flush();

            if (task.exception != null)
                throw task.exception;
        } finally {
            Buffer.release(buffers);
            if (interrupted)
                Thread.currentThread().interrupt();
        }
    }

    /** A buffer for I/O. */
    private static final class Buffer {
        /**
         * Each entry in this list holds a soft reference to an array
         * initialized with instances of this class.
         */
        static final List<Reference<Buffer[]>> list = new LinkedList<Reference<Buffer[]>>();

        static Buffer[] allocate() {
            synchronized (Buffer.class) {
                Buffer[] buffers;
                final Iterator<Reference<Buffer[]>> i = list.iterator();
                while (i.hasNext()) {
                    buffers = i.next().get();
                    i.remove();
                    if (null != buffers)
                        return buffers;
                }
            }

            final Buffer[] buffers = new Buffer[FIFO_SIZE];
            for (int i = buffers.length; --i >= 0; )
                buffers[i] = new Buffer();
            return buffers;
        }

        static synchronized void release(Buffer[] buffers) {
            list.add(new SoftReference<Buffer[]>(buffers));
        }

        /** The byte buffer used for reading and writing. */
        final byte[] buf = new byte[BUFFER_SIZE];

        /** The actual number of bytes read into the buffer. */
        int read;
    } // Buffer

    /** A factory for reader threads. */
    private static final class ReaderThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable r) {
            return new ReaderThread(r);
        }
    } // ReaderThreadFactory

    /**
     * A pooled and cached daemon thread which reads input streams.
     * You cannot use this class outside its package.
     */
    public static final class ReaderThread extends Thread {
        ReaderThread(Runnable r) {
            super(ThreadGroups.getServerThreadGroup(), r, ReaderThread.class.getName());
            setDaemon(true);
        }
    } // ReaderThread
}
