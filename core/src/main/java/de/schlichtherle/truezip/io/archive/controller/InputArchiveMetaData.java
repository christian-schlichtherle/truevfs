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

package de.schlichtherle.truezip.io.archive.controller;

import de.schlichtherle.truezip.io.archive.Archive;
import de.schlichtherle.truezip.io.archive.driver.InputArchive;
import de.schlichtherle.truezip.io.socket.IOReferences;
import de.schlichtherle.truezip.io.SynchronizedInputStream;
import de.schlichtherle.truezip.io.archive.driver.ArchiveEntry;
import de.schlichtherle.truezip.io.socket.IOReference;
import de.schlichtherle.truezip.util.ExceptionHandler;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Annotates an {@link InputArchive} with the methods required for safe
 * reading of archive entries.
 * As an implication of this, it's also responsible for the synchronization
 * of the streams between multiple threads.
 * <p>
 * <b>Warning:</b> This class is <em>not</em> intended for public use!
 * It's only public for technical reasons and may get renamed or entirely
 * disappear without notice.
 *
 * @see OutputArchiveMetaData
 * @author Christian Schlichtherle
 * @version $Id$
 */
// TODO: Make this class package private!
public final class InputArchiveMetaData {

    private static final String CLASS_NAME
            = InputArchiveMetaData.class.getName();
    private static final Logger logger
            = Logger.getLogger(CLASS_NAME, CLASS_NAME);

    /**
     * The archive which uses this instance.
     * Although this field is actually never used in this class, it makes an
     * archive controller strongly reachable from any entry stream in use by
     * any thread.
     * This is required to keep the archive controller from being garbarge
     * collected meanwhile.
     * <p>
     * <b>Detail:</b> While this is really required for input streams for
     * archives which are unmodified, it's actually not required for output
     * streams, since the archive file system is touched for these streams
     * anyway, which in turn schedules the archive controller for the next
     * update, which in turn prevents it from being garbage collected.
     * However, it's provided for symmetry between input archive meta data
     * and output archive meta data.
     */
    //private final Archive archive;

    private final InputArchive inArchive;

    /**
     * The pool of all open entry streams.
     * This is implemented as a map where the keys are the streams and the
     * value is the current thread.
     * If {@code File.isLenient()} is true, then the map is actually
     * instantiated as a {@link WeakHashMap}. Otherwise, it's a {@link HashMap}.
     * The weak hash map allows the garbage collector to pick up an entry
     * stream if there are no more references to it.
     * This reduces the likeliness of an exception in case a sloppy client
     * application has forgot to close a stream before the target archive file
     * gets updated.
     */
    private final Map<EntryInputStream, Thread> streams
            = ArchiveControllers.isLenient()
            ? new WeakHashMap<EntryInputStream, Thread>()
            : new HashMap<EntryInputStream, Thread>();

    private volatile boolean stopped;

    /**
     * Creates a new instance of {@code InputArchiveMetaData}
     * and sets itself as the meta data for the given input archive.
     */
    InputArchiveMetaData(final Archive archive, final InputArchive inArchive) {
        assert inArchive != null;

        //this.archive = archive;
        this.inArchive = inArchive;
    }

    synchronized InputStream newInputStream(
            final IOReference<? extends ArchiveEntry> targetRef,
            final IOReference<? extends ArchiveEntry> peerRef)
            throws IOException {
        assert !stopped;
        assert targetRef != null;

        return new EntryInputStream(
                inArchive
                .getInputStreamSocket(IOReferences.deref(targetRef))
                .newInputStream(peerRef));
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
    synchronized int waitAllInputStreamsByOtherThreads(final long timeout) {
        assert !stopped;

        final long start = System.currentTimeMillis();
        final int threadStreams = threadStreams();
        //Thread.interrupted(); // cancel pending interrupt
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
                if (ArchiveControllers.isLenient()) {
                    System.gc(); // trigger garbage collection
                    System.runFinalization(); // trigger finalizers - is this required at all?
                }
                wait(toWait);
            }
        } catch (InterruptedException ignored) {
            logger.warning("interrupted");
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
    synchronized <E extends Exception>
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
     * {@link InputArchive}.
     * This input stream provides support for finalization and throws an
     * {@link IOException} on any subsequent attempt to read data after
     * {@link #closeAllInputStreams} has been called.
     */
    private final class EntryInputStream extends SynchronizedInputStream {
        private /*volatile*/ boolean closed;

        @SuppressWarnings({ "NotifyWhileNotSynced", "LeakingThisInConstructor" })
        private EntryInputStream(final InputStream in) {
            super(in, InputArchiveMetaData.this);
            assert in != null;
            streams.put(this, Thread.currentThread());
            InputArchiveMetaData.this.notify(); // there can be only one waiting thread!
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
            assert InputArchiveMetaData.this == lock;
            synchronized (InputArchiveMetaData.this) {
                if (closed)
                    return;

                // Order is important!
                try {
                    doClose();
                } finally {
                    streams.remove(this);
                    InputArchiveMetaData.this.notify(); // there can be only one waiting thread!
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
                    logger.log(Level.FINE, "finalize.exception", failure);
                }
            } finally {
                super.finalize();
            }
        }
    } // class EntryInputStream
}
