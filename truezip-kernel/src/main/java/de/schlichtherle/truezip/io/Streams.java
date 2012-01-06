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
import java.util.Deque;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import net.jcip.annotations.ThreadSafe;

/**
 * Provides static copy methods for {@link InputStream}s and
 * {@link OutputStream}s.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@DefaultAnnotation(NonNull.class)
public final class Streams {

    /**
     * The size of the FIFO used for exchanging I/O buffers between a reader
     * thread and a writer thread.
     * A minimum of two elements is required.
     * The actual number is optimized to compensate for oscillating I/O
     * bandwidths like e.g. with network shares.
     */
    static final int FIFO_SIZE = 4;

    /** The buffer size used for reading and writing, which is {@value}. */
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

        // We will use a FIFO to exchange byte buffers between a pooled reader
        // thread and the current writer thread.
        // The pooled reader thread will fill the buffers with data from the
        // input and the current thread will write the filled buffers to the
        // output.
        // The FIFO is simply implemented as a cached array or byte buffers
        // with an offset and a size which is used like a ring buffer.

        final Lock mutex = new ReentrantLock();
        final Condition signal = mutex.newCondition();
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
                    mutex.lock();
                    try {
                        while (size >= _buffersLen) {
                            try {
                                signal.await();
                            } catch (InterruptedException interrupted) {
                                // The writer thread wants us to stop reading.
                                return;
                            }
                        }
                        buffer = _buffers[(off + size) % _buffersLen];
                    } finally {
                        mutex.unlock();
                    }

                    // Fill buffer until end of file or buffer.
                    // This should normally complete in one loop cycle, but
                    // we do not depend on this as it would be a violation
                    // of InputStream's contract.
                    try {
                        final byte[] buf = buffer.buf;
                        read = _in.read(buf, 0, buf.length);
                    } catch (IOException ex) {
                        exception = new InputException(ex);
                        read = -1;
                    }
                    buffer.read = read;

                    // Advance head and signal writer.
                    mutex.lock();
                    try {
                        size++;
                        signal.signal(); // only the writer could be waiting now!
                    } finally {
                        mutex.unlock();
                    }
                } while (0 <= read);
            }
        } // ReaderTask

        boolean interrupted = false;
        try {
            final ReaderTask reader = new ReaderTask();
            final Future<?> result = executor.submit(reader);

            // Cache some data for better performance.
            final int buffersLen = buffers.length;

            int write;
            while (true) {
                // Wait until a buffer is available.
                final int off;
                final Buffer buffer;
                mutex.lock();
                try {
                    while (0 >= reader.size) {
                        try {
                            signal.await();
                        } catch (InterruptedException ex) {
                            interrupted = true;
                        }
                    }
                    off = reader.off;
                    buffer = buffers[off];
                } finally {
                    mutex.unlock();
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

                // Advance tail and signal reader.
                mutex.lock();
                try {
                    reader.off = (off + 1) % buffersLen;
                    reader.size--;
                    signal.signal(); // only the reader could be waiting now!
                } finally {
                    mutex.unlock();
                }
            }
            out.flush();

            if (reader.exception != null)
                throw reader.exception;
        } finally {
            Buffer.release(buffers);
            if (interrupted)
                Thread.currentThread().interrupt();
        }
    }

    /** A buffer for I/O. */
    private static final class Buffer {
        /**
         * Each entry in this queue holds a soft reference to an array
         * initialized with instances of this class.
         * <p>
         * The best choice would be a {@link ConcurrentLinkedDeque} where I
         * could call {@link Deque#push(Object)} to achieve many garbage
         * collector pickups of old {@link SoftReference}s further down the
         * stack, but this class is only available since JSE 7.
         * A {@link LinkedBlockingDeque} is supposedly not a good choice
         * because it uses locks, which I would like to abandon.
         */
        static final Queue<Reference<Buffer[]>> queue
                = new ConcurrentLinkedQueue<Reference<Buffer[]>>();

        static Buffer[] allocate() {
            {
                Reference<Buffer[]> reference;
                while (null != (reference = queue.poll())) {
                    final Buffer[] buffers = reference.get();
                    if (null != buffers)
                        return buffers;
                }
            }

            final Buffer[] buffers = new Buffer[FIFO_SIZE];
            for (int i = buffers.length; 0 <= --i; )
                buffers[i] = new Buffer();
            return buffers;
        }

        static void release(Buffer[] buffers) {
            //queue.push(new SoftReference<Buffer[]>(buffers));
            queue.add(new SoftReference<Buffer[]>(buffers));
        }

        /** The byte buffer used for reading and writing. */
        final byte[] buf = new byte[BUFFER_SIZE];

        /**
         * The actual number of bytes read into the buffer.
         * -1 represents end-of-file or {@link IOException}.
         */
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
     * A pooled and cached daemon thread which runs tasks to read input streams.
     * You cannot use this class outside its package.
     */
    public static final class ReaderThread extends Thread {
        ReaderThread(Runnable r) {
            super(ThreadGroups.getServerThreadGroup(), r, ReaderThread.class.getName());
            setDaemon(true);
        }
    } // ReaderThread
}
