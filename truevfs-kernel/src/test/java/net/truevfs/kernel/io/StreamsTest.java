/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.io;

import net.truevfs.kernel.TestConfig;
import net.truevfs.kernel.ThrowManager;
import net.truevfs.kernel.cio.ByteArrayIoBuffer;
import static net.truevfs.kernel.util.Throwables.contains;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Christian Schlichtherle
 */
public class StreamsTest {

    private static final int
            BUFFER_SIZE = 2 * Streams.FIFO_SIZE * Streams.BUFFER_SIZE;
    private static final Random rnd = new Random();

    private ByteArrayIoBuffer buffer;

    @Before
    public void setUp() {
        TestConfig.push();
        buffer = newByteArrayIOBuffer();
    }

    @After
    public void tearDown() {
        TestConfig.pop();
    }

    private static ByteArrayIoBuffer newByteArrayIOBuffer() {
        final byte[] data = new byte[BUFFER_SIZE];
        rnd.nextBytes(data);
        return new ByteArrayIoBuffer("test", data);
    }

    private static TestInputStream newTestInputStream(ByteArrayIoBuffer buffer)
    throws IOException {
        return new TestInputStream(buffer.input().stream());
    }

    private static TestOutputStream newTestOutputStream(ByteArrayIoBuffer buffer)
    throws IOException {
        return new TestOutputStream(buffer.output().stream());
    }

    @Test
    public void testCat() throws IOException {
        new CatTest() {
            @Override
            void cat(final TestInputStream in, final TestOutputStream out)
            throws IOException {
                Streams.cat(in, out);
                Streams.cat(in, out); // repeated calls don't matter
            }
        }.run();
    }

    @Test
    public void testCatInputException() throws IOException {
        assertCatInputException(new IOException());
        assertCatInputException(new RuntimeException());
        assertCatInputException(new Error());
    }

    private void assertCatInputException(final Throwable expected)
    throws IOException {
        new CatTest() {
            @Override
            void cat(final TestInputStream in, final TestOutputStream out)
            throws IOException {
                final ThrowManager control = TestConfig.get().getThrowControl();
                control.trigger(ThrowingInputStream.class, expected);
                final ThrowingInputStream
                        tis = new ThrowingInputStream(in, control);
                try {
                    Streams.cat(tis, out);
                    fail();
                } catch (final IOException | RuntimeException | Error got) {
                    if (!contains(got, expected))
                        throw got;
                }
                Streams.cat(in, out);
            }
        }.run();
    }

    @Test
    public void testCatOutputException() throws IOException {
        assertCatOutputException(new IOException());
        assertCatOutputException(new RuntimeException());
        assertCatOutputException(new Error());
    }

    private void assertCatOutputException(final Throwable expected)
    throws IOException {
        new CatTest() {
            @Override
            void cat(final TestInputStream in, final TestOutputStream out)
            throws IOException {
                final ThrowManager control = TestConfig.get().getThrowControl();
                control.trigger(ThrowingOutputStream.class, expected);
                final ThrowingOutputStream tos = new ThrowingOutputStream(out);
                Streams.cat(in, out);
                try {
                    Streams.cat(in, tos);
                    fail();
                } catch (final IOException | RuntimeException | Error got) {
                    if (!contains(got, expected))
                        throw got;
                }
            }
        }.run();
    }

    @Test
    public void testCopy() throws IOException {
        assertCopy(buffer);
    }

    private void assertCopy(final ByteArrayIoBuffer buffer)
    throws IOException {
        final byte[] data = buffer.getData();
        final TestInputStream in = newTestInputStream(buffer);
        final TestOutputStream out = newTestOutputStream(buffer);
        Thread.currentThread().interrupt();
        Streams.copy(in, out);
        assertTrue(Thread.interrupted()); // test and clear status!
        assertTrue(out.flushed);
        assertTrue(out.closed);
        assertTrue(in.closed);
        assertNotSame(data, buffer.getData());
        assertArrayEquals(data, buffer.getData());
    }

    @Test
    public void testMultithreadedCopying() throws Exception {
        class Task implements Callable<Void> {
            @Override
            public Void call() throws IOException {
                assertCopy(newByteArrayIOBuffer());
                return null;
            }
        } // Task

        final int numThreads = Runtime.getRuntime().availableProcessors() * 10;
        final int numTasks = 10 * numThreads;

        final List<Future<Void>> results = new ArrayList<>(numTasks);
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

    private static abstract class CatTest {
        final byte[] data = new byte[BUFFER_SIZE];
        { rnd.nextBytes(data); }
        final ByteArrayIoBuffer buffer = new ByteArrayIoBuffer("test", data);

        void run() throws IOException {
            try (   final TestInputStream in = newTestInputStream(buffer);
                    final TestOutputStream out = newTestOutputStream(buffer)) {
                Thread.currentThread().interrupt();
                cat(in, out);
                assertTrue("The interrupt status should not have changed!",
                        Thread.interrupted());
                assertTrue(out.flushed);
                assertFalse(out.closed);
                assertFalse(in.closed);
            }
            assertNotSame(data, buffer.getData());
            assertArrayEquals(data, buffer.getData());
        }

        abstract void cat(  final TestInputStream in,
                            final TestOutputStream out)
        throws IOException;
    } // Cat

    private static class TestInputStream extends DecoratingInputStream {
        boolean closed;

        TestInputStream(final InputStream in) {
            super(in);
        }

        @Override
        public int read() throws IOException {
            throw new AssertionError();
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
            in.close();
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

    private static class TestOutputStream extends DecoratingOutputStream {
        boolean flushed;
        boolean closed;

        TestOutputStream(final OutputStream out) {
            super(out);
        }

        @Override
        public void write(int b) throws IOException {
            throw new AssertionError();
        }

        @Override
        public void flush() throws IOException {
            out.flush();
            flushed = true;
        }

        @Override
        public void close() throws IOException {
            out.close();
            closed = true;
        }
    }
}
