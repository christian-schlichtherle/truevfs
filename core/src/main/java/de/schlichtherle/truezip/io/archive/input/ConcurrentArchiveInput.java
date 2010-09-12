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

package de.schlichtherle.truezip.io.archive.input;

import de.schlichtherle.truezip.io.SynchronizedInputStream;
import de.schlichtherle.truezip.io.archive.controller.ArchiveBusyWarningException;
import de.schlichtherle.truezip.io.archive.controller.ArchiveControllers;
import de.schlichtherle.truezip.io.archive.controller.ArchiveEntryStreamClosedException;
import de.schlichtherle.truezip.io.archive.entry.ArchiveEntry;
import de.schlichtherle.truezip.io.archive.output.ConcurrentArchiveOutput;
import de.schlichtherle.truezip.util.ExceptionHandler;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Decorates an {@code ArchiveInput} to add accounting and multithreading
 * synchronization for all input streams created by the target archive
 * input.
 *
 * @param   <AE> The type of the archive entries.
 * @see ConcurrentArchiveOutput
 * @author Christian Schlichtherle
 * @version $Id$
 */
public final class ConcurrentArchiveInput<AE extends ArchiveEntry>
extends FilterArchiveInput<AE> {

    private static final String CLASS_NAME
            = ConcurrentArchiveInput.class.getName();
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
     * the target archive file gets synchronized, e.g. by using
     * {@link ArchiveControllers#sync(java.net.URI, SyncConfiguration) umount}.
     */
    private final Map<EntryInputStream, Thread> streams
            = new WeakHashMap<EntryInputStream, Thread>();

    private volatile boolean stopped;

    /** Constructs a new instance of {@code ConcurrentArchiveInput}. */
    private ConcurrentArchiveInput(final ArchiveInput<AE> input) {
        super(input);
    }

    /**
     * Returns a new concurrent archive input which decorates (wraps) the
     * given non-{@code null} archive input.
     */
    public static <AE extends ArchiveEntry>
    ConcurrentArchiveInput<AE> wrap(ArchiveInput<AE> archive) {
        return new ConcurrentArchiveInput<AE>(archive);
    }

    /**
     * Returns the wrapped archive input or {@code null} if and only if
     * {@code proxy} is {@code null}.
     */
    public static <AE extends ArchiveEntry>
    ArchiveInput<AE> unwrap(ConcurrentArchiveInput<AE> proxy) {
        return proxy != null ? proxy.target : null;
    }

    @Override
    public ArchiveInputStreamSocket<? extends AE> getInputStreamSocket(
            final AE entry)
    throws FileNotFoundException {
        assert !stopped;
        assert entry != null;

        // TODO: Consider synchronization!
        final ArchiveInputStreamSocket<? extends AE> input
                = target.getInputStreamSocket(entry);
        class InputStreamSocket implements ArchiveInputStreamSocket<AE> {
            @Override
            public AE get() {
                return entry;
            }

            @Override
            public InputStream newInputStream(final ArchiveEntry dst)
            throws IOException {
                synchronized (ConcurrentArchiveInput.this) {
                    return new EntryInputStream(input.newInputStream(dst));
                }
            }
        }
        return new InputStreamSocket();
    }

    /**
     * Waits until all entry streams which have been opened (and not yet closed)
     * by all <em>other threads</em> are closed or a timeout occurs.
     * If the current thread is interrupted while waiting,
     * a warning message is logged using {@code java.util.logging} and
     * this method returns.
     * <p>
     * Unless otherwise prevented, another thread could immediately open
     * another stream upon return of this method.
     * So there is actually no guarantee that really <em>all</em> streams
     * are closed upon return of this method - use carefully!
     *
     * @return The number of all open streams.
     */
    public synchronized int waitCloseAllInputStreams(final long timeout) {
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

    /** Returns the number of streams opened by the current thread. */
    private int threadStreams() {
        final Thread thisThread = Thread.currentThread();
        int n = 0;
        for (final Thread thread : streams.values())
            if (thisThread == thread)
                n++;
        return n;
    }

    /**
     * Closes and disconnects <em>all</em> entry streams for the target archive
     * file.
     * <i>Disconnecting</i> means that any subsequent operation on the entry
     * streams will throw an {@code IOException}, with the exception of
     * their {@code close()} method.
     */
    public synchronized <E extends Exception>
    void closeAllInputStreams(final ExceptionHandler<IOException, E> handler)
    throws E {
        assert !stopped;
        stopped = true;
        for (final Iterator<EntryInputStream> it = streams.keySet().iterator();
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
     * An {@link InputStream} to read the entry data from an
     * {@link ArchiveInput}.
     * This input stream provides support for finalization and throws an
     * {@link IOException} on any subsequent attempt to read data after
     * {@link #closeAllInputStreams} has been called.
     */
    private final class EntryInputStream extends SynchronizedInputStream {
        private /*volatile*/ boolean closed;

        @SuppressWarnings({ "NotifyWhileNotSynced", "LeakingThisInConstructor" })
        private EntryInputStream(final InputStream in) {
            super(in, ConcurrentArchiveInput.this);
            assert in != null;
            streams.put(this, Thread.currentThread());
            ConcurrentArchiveInput.this.notify(); // there can be only one waiting thread!
        }

        private void ensureNotStopped() throws IOException {
            if (stopped)
                throw new ArchiveEntryStreamClosedException();
        }

        @Override
        public int read() throws IOException {
            ensureNotStopped();
            return super.read();
        }

        @Override
        public int read(byte[] b) throws IOException {
            ensureNotStopped();
            return super.read(b);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            ensureNotStopped();
            return super.read(b, off, len);
        }

        @Override
        public long skip(long n) throws IOException {
            ensureNotStopped();
            return super.skip(n);
        }

        @Override
        public int available() throws IOException {
            ensureNotStopped();
            return super.available();
        }

        @Override
        public void mark(int readlimit) {
            if (!stopped)
                super.mark(readlimit);
        }

        @Override
        public void reset() throws IOException {
            ensureNotStopped();
            super.reset();
        }

        @Override
        public boolean markSupported() {
            return !stopped && super.markSupported();
        }

        /**
         * Closes this archive entry stream and releases any resources
         * associated with it.
         * This method tolerates multiple calls to it: Only the first
         * invocation closes the underlying stream.
         *
         * @throws IOException If an I/O exception occurs.
         */
        @Override
        public final void close() throws IOException {
            assert ConcurrentArchiveInput.this == lock;
            synchronized (ConcurrentArchiveInput.this) {
                if (closed)
                    return;
                // Order is important!
                try {
                    doClose();
                } finally {
                    streams.remove(this);
                    ConcurrentArchiveInput.this.notify(); // there can be only one waiting thread!
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
         * The finalizer in this class forces this archive entry input
         * stream to close.
         * This is used to ensure that an archive can be updated although
         * the client may have "forgot" to close this input stream before.
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
    } // class EntryInputStream
}
