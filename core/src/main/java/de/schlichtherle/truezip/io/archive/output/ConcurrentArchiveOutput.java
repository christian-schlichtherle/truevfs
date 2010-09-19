/*
 * Copyright (C) 2006-2010 Schlichtherle IT Services
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

package de.schlichtherle.truezip.io.archive.output;

import de.schlichtherle.truezip.io.SynchronizedOutputStream;
import de.schlichtherle.truezip.io.archive.controller.ArchiveBusyWarningException;
import de.schlichtherle.truezip.io.archive.controller.ArchiveEntryStreamClosedException;
import de.schlichtherle.truezip.io.archive.entry.ArchiveEntry;
import de.schlichtherle.truezip.io.archive.input.ConcurrentArchiveInput;
import de.schlichtherle.truezip.util.ExceptionHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Decorates an {@code ArchiveOutput} to add accounting and multithreading
 * synchronization for all output streams created by the target archive
 * output.
 *
 * @param   <AE> The type of the archive entries.
 * @see ConcurrentArchiveInput
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class ConcurrentArchiveOutput<
        AE extends ArchiveEntry,
        AO extends ArchiveOutput<AE>>
extends FilterArchiveOutput<AE, AO> {

    private static final String CLASS_NAME
            = ConcurrentArchiveOutput.class.getName();
    private static final Logger logger
            = Logger.getLogger(CLASS_NAME, CLASS_NAME);

    /**
     * The pool of all open entry streams.
     * This is implemented as a map where the keys are the streams and the
     * value is the current thread.
     * The weak hash map allows the garbage collector to pick up an entry
     * stream if there are no more references to it.
     * This reduces the likeliness of an {@link ArchiveBusyWarningException}
     * in case a sloppy client application has forgot to close a stream before
     * the target archive file gets synchronized.
     */
    private final Map<EntryOutputStream, Thread> streams
            = new WeakHashMap<EntryOutputStream, Thread>();

    private volatile boolean stopped;

    /** Constructs a new {@code ConcurrentArchiveOutput}. */
    public ConcurrentArchiveOutput(final AO target) {
        super(target);
    }

    @Override
    public ArchiveOutputStreamSocket<? extends AE> getOutputStreamSocket(
            final AE entry)
    throws IOException {
        assert !stopped;
        assert entry != null;

        // TODO: Consider synchronization!
        final ArchiveOutputStreamSocket<? extends AE> output
                = target.getOutputStreamSocket(entry);
        class OutputStreamSocket implements ArchiveOutputStreamSocket<AE> {
            @Override
            public AE getTarget() {
                return entry;
            }

            @Override
            public OutputStream newOutputStream(final ArchiveEntry input)
            throws IOException {
                synchronized (ConcurrentArchiveOutput.this) {
                    return new EntryOutputStream(output.newOutputStream(input));
                }
            }
        }
        return new OutputStreamSocket();
    }

    /**
     * Waits until all entry streams which have been opened (and not yet closed)
     * by all <em>other threads</em> are closed or a timeout occurs.
     * If the current thread is interrupted while waiting,
     * a warn message is logged using {@code java.util.logging} and
     * this method returns.
     * <p>
     * Unless otherwise prevented, another thread could immediately open
     * another stream upon return of this method.
     * So there is actually no guarantee that really <em>all</em> streams
     * are closed upon return of this method - use carefully!
     *
     * @return The number of all open streams.
     */
    public synchronized int waitCloseAllOutputStreams(final long timeout) {
        assert !stopped;

        final long start = System.currentTimeMillis();
        final int threadStreams = threadStreams();
        try {
            while (streams.size() > threadStreams) {
                long toWait;
                if (timeout > 0) {
                    toWait = timeout - (System.currentTimeMillis() - start);
                    if (toWait <= 0)
                        break;
                } else {
                    toWait = 0;
                }
                System.gc(); // trigger garbage collection
                System.runFinalization(); // trigger finalizers - is this required at all?
                wait(toWait);
            }
        } catch (InterruptedException ignored) {
            logger.warning("wait.interrupted");
        }

        return streams.size();
    }

    /**
     * Returns the number of streams opened by the current thread.
     */
    private int threadStreams() {
        final Thread thisThread = Thread.currentThread();
        int n = 0;
        for (final Thread thread : streams.values())
            if (thisThread == thread)
                n++;
        return n;
    }

    /**
     * Closes and disconnects <em>all</em> entry streams for the archive
     * containing this metadata object.
     * <i>Disconnecting</i> means that any subsequent operation on the entry
     * streams will throw an {@code IOException}, with the exception of
     * their {@code close()} method.
     */
    public synchronized <E extends Exception>
    void closeAllOutputStreams(final ExceptionHandler<IOException, E> handler)
    throws E {
        assert !stopped;
        stopped = true;
        for (final Iterator<EntryOutputStream> it = streams.keySet().iterator();
        it.hasNext(); ) {
            try {
                try {
                    it.next().doClose();
                } finally {
                    it.remove();
                }
            } catch (IOException ioe) {
                handler.warn(ioe);
            }
        }
    }

    /**
     * An {@link OutputStream} to write the entry data to an
     * {@link ArchiveOutput}.
     * This output stream provides support for finalization and throws an
     * {@link IOException} on any subsequent attempt to write data after
     * {@link #closeAllOutputStreams} has been called.
     */
    private final class EntryOutputStream extends SynchronizedOutputStream {
        private /*volatile*/ boolean closed;

        @SuppressWarnings({ "NotifyWhileNotSynced", "LeakingThisInConstructor" })
        private EntryOutputStream(final OutputStream out) {
            super(out, ConcurrentArchiveOutput.this);
            assert out != null;
            streams.put(this, Thread.currentThread());
            ConcurrentArchiveOutput.this.notify(); // there can be only one waiting thread!
        }

        private void ensureNotStopped() throws IOException {
            if (stopped)
                throw new ArchiveEntryStreamClosedException();
        }

        @Override
        public void write(int b) throws IOException {
            ensureNotStopped();
            super.write(b);
        }

        @Override
        public void write(byte[] b) throws IOException {
            ensureNotStopped();
            super.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            ensureNotStopped();
            super.write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            ensureNotStopped();
            super.flush();
        }

        /**
         * Closes this archive entry stream and releases any resources
         * associated with it.
         * This method tolerates multiple calls to it: Only the first
         * invocation flushes and closes the underlying stream.
         *
         * @throws IOException If an I/O exception occurs.
         */
        @Override
        public final void close() throws IOException {
            assert ConcurrentArchiveOutput.this == lock;
            synchronized (ConcurrentArchiveOutput.this) {
                if (closed)
                    return;
                // Order is important!
                try {
                    doClose();
                } finally {
                    streams.remove(this);
                    ConcurrentArchiveOutput.this.notify(); // there can be only one waiting thread!
                }
            }
        }

        /**
         * Closes the underlying stream and marks this stream as being closed.
         * It is an fail to call this method on an already closed stream.
         * This method does <em>not</em> remove this stream from the pool.
         * This method is not synchronized!
         *
         * @throws IOException If an I/O exception occurs.
         */
        @Override
        protected void doClose() throws IOException {
            assert !closed;
            /*if (closed)
                return;*/
            // Order is important!
            closed = true;
            super.doClose();
        }

        /**
         * The finalizer in this class forces this archive entry output
         * stream to close.
         * This is used to ensure that an archive can be updated although
         * the client may have "forgot" to close this output stream before.
         */
        @Override
        @SuppressWarnings("FinalizeDeclaration")
        protected void finalize() throws Throwable {
            try {
                if (closed)
                    return;
                logger.finer("finalize.open");
                try {
                    doClose();
                } catch (IOException failure) {
                    logger.log(Level.FINE, "finalize.catch", failure);
                }
            } finally {
                super.finalize();
            }
        }
    } // class EntryOutputStream
}
