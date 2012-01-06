/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public class StreamsTest {

    @Test
    public void testCat() throws IOException {
        final MockInputStream in = new MockInputStream();
        final MockOutputStream out = new MockOutputStream(in);
        Thread.currentThread().interrupt();
        try {
            Streams.cat(in, out);
            fail();
        } catch (InputException expected) {
            assertTrue(expected.getCause() instanceof EOFException);
        }
        assertTrue(Thread.interrupted());
        assertTrue(out.flushed);
        assertFalse(out.closed);
        assertFalse(in.closed);
        assertArrayEquals(in.buffer, out.toByteArray());
    }

    @Test
    public void testOutputException() throws IOException {
        final MockInputStream in = new MockInputStream();
        final MockOutputStream out = new MockOutputStream(in) {
            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                throw new EOFException();
            }
        };
        Thread.currentThread().interrupt();
        try {
            Streams.cat(in, out);
            fail();
        } catch (EOFException expected) {
        }
        assertTrue(Thread.interrupted());
        assertFalse(out.flushed);
        assertFalse(out.closed);
        assertFalse(in.closed);
    }

    @Test
    public void testCopy() throws IOException {
        final MockInputStream in = new MockInputStream();
        final MockOutputStream out = new MockOutputStream(in);
        Thread.currentThread().interrupt();
        try {
            Streams.copy(in, out);
            fail();
        } catch (InputException expected) {
            assertTrue(expected.getCause() instanceof EOFException);
        }
        assertTrue(Thread.interrupted());
        assertTrue(out.flushed);
        assertTrue(out.closed);
        assertTrue(in.closed);
        assertArrayEquals(in.buffer, out.toByteArray());
    }

    @Test
    public void testMultithreading() throws Exception {
        class Task implements Callable<Void> {
            @Override
            public Void call() throws IOException {
                testCopy();
                return null;
            }
        } // Task

        final int numThreads = Runtime.getRuntime().availableProcessors() * 10;
        final int numTasks = 10 * numThreads;

        final List<Future<Void>> results = new ArrayList<Future<Void>>(numTasks);
        final ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        try {
            for (int i = 0; i < numTasks; i++)
                results.add(executor.submit(new Task()));
        } finally {
            executor.shutdown();
        }
        for (final Future<Void> result : results)
            result.get(); // check out exception
    }

    private static class MockInputStream extends DecoratingInputStream {
        final byte[] buffer;
        boolean closed;

        MockInputStream() { this(newBuffer()); }

        MockInputStream(final byte[] buffer) {
            super(new ByteArrayInputStream(buffer));
            this.buffer = buffer;
        }

        static byte[] newBuffer() {
            final byte[] buffer = new byte[2 * Streams.FIFO_SIZE * Streams.BUFFER_SIZE];
            ThreadLocalRandom.current().nextBytes(buffer);
            return buffer;
        }

        @Override
        public int read() throws IOException {
            throw new AssertionError();
        }

        @Override
        public int read(final byte[] b, final int off, final int len)
        throws IOException {
            final int read = delegate.read(b, off, len);
            if (0 > read)
                throw new EOFException();
            return read;
        }

        @Override
        public long skip(long n) throws IOException {
            throw new AssertionError();
        }

        @Override
        public int available() throws IOException {
            throw new AssertionError();
        }

        @Override
        public void close() throws IOException {
            delegate.close();
            closed = true;
        }

        @Override
        public void mark(int readlimit) {
            throw new AssertionError();
        }

        @Override
        public void reset() throws IOException {
            throw new AssertionError();
        }

        @Override
        public boolean markSupported() {
            throw new AssertionError();
        }
    }

    private static class MockOutputStream extends DecoratingOutputStream {
        boolean closed;
        boolean flushed;

        MockOutputStream(final MockInputStream in) {
            super(new ByteArrayOutputStream(in.buffer.length));
        }

        @Override
        public void write(int b) throws IOException {
            throw new AssertionError();
        }

        /*@Override
        public void write(byte[] b, int off, int len) throws IOException {
            delegate.write(b, off, len);
        }*/

        @Override
        public void flush() throws IOException {
            delegate.flush();
            flushed = true;
        }

        @Override
        public void close() throws IOException {
            delegate.close();
            closed = true;
        }

        byte[] toByteArray() {
            return ((ByteArrayOutputStream) delegate).toByteArray();
        }
    }
}
