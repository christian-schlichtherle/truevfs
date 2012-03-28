/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.kernel.fs;

import de.schlichtherle.truezip.kernel.fs.FsCache.Strategy;
import static de.schlichtherle.truezip.kernel.fs.FsCache.Strategy.WRITE_BACK;
import static de.schlichtherle.truezip.kernel.fs.FsCache.Strategy.WRITE_THROUGH;
import static de.truezip.kernel.cio.Entry.Access.READ;
import static de.truezip.kernel.cio.Entry.Access.WRITE;
import static de.truezip.kernel.cio.Entry.Size.DATA;
import de.truezip.kernel.cio.*;
import de.truezip.kernel.rof.ReadOnlyFile;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Christian Schlichtherle
 */
public final class FsCacheTest {

    private static final int INITIAL_CAPACITY = 32;
    private static final String MOCK_ENTRY_NAME = "mock";
    private static final String MOCK_ENTRY_DATA_READ = "read";
    private static final String MOCK_ENTRY_DATA_WRITE = "write";

    private ByteArrayIOPool pool;

    @Before
    public void setUp() throws IOException {
        pool = new ByteArrayIOPool(5);
    }

    @Test
    public void testCaching() throws IOException {
        for (final Strategy strategy : new Strategy[] {
            WRITE_THROUGH,
            WRITE_BACK,
        }) {
            final FsCache cache = strategy.newCache(pool);
            ByteArrayIOBuffer front;
            ByteArrayIOBuffer back;

            back = new ByteArrayIOBuffer(MOCK_ENTRY_NAME, INITIAL_CAPACITY);
            back.setData(MOCK_ENTRY_DATA_READ.getBytes());
            cache   .configure(new BrokenInputSocket(back))
                    .configure(new BrokenOutputSocket(back));
            assertThat(pool.size(), is(0));
            assertThat(new String(back.getData()), equalTo(MOCK_ENTRY_DATA_READ));
            assertThat(back.getCount(READ), is(0));
            assertThat(back.getCount(WRITE), is(0));
            assertThat(cache.getEntry(), nullValue());

            front = new ByteArrayIOBuffer(MOCK_ENTRY_NAME, INITIAL_CAPACITY);
            assertThat(front.getData(), nullValue());
            try {
                IOSocket.copy(cache.getInputSocket(), front.getOutputSocket());
                fail();
            } catch (IOException expected) {
            }
            assertThat(pool.size(), is(0));
            assertThat(front.getData(), nullValue());
            assertThat(new String(back.getData()), equalTo(MOCK_ENTRY_DATA_READ));
            assertThat(back.getCount(READ), is(0));
            assertThat(back.getCount(WRITE), is(0));
            assertThat(cache.getEntry(), nullValue());

            cache   .configure(back.getInputSocket())
                    .configure(back.getOutputSocket());
            assertThat(pool.size(), is(0));
            assertThat(front.getData(), nullValue());
            assertThat(new String(back.getData()), equalTo(MOCK_ENTRY_DATA_READ));
            assertThat(back.getCount(READ), is(0));
            assertThat(back.getCount(WRITE), is(0));
            assertThat(cache.getEntry(), nullValue());

            front = new ByteArrayIOBuffer(MOCK_ENTRY_NAME, INITIAL_CAPACITY);
            assertThat(pool.size(), is(0));
            assertThat(front.getData(), nullValue());
            IOSocket.copy(cache.getInputSocket(), front.getOutputSocket());
            assertThat(pool.size(), is(1));
            assertThat(new String(front.getData()), equalTo(MOCK_ENTRY_DATA_READ));
            assertThat(new String(back.getData()), equalTo(MOCK_ENTRY_DATA_READ));
            assertThat(back.getCount(READ), is(1));
            assertThat(back.getCount(WRITE), is(0));
            assertThat(cache.getEntry(), notNullValue());
            assertThat(cache.getEntry().getSize(DATA), equalTo((long) MOCK_ENTRY_DATA_READ.length()));

            front = new ByteArrayIOBuffer(MOCK_ENTRY_NAME, INITIAL_CAPACITY);
            front.setData(MOCK_ENTRY_DATA_WRITE.getBytes());
            cache   .configure(new BrokenInputSocket(back))
                    .configure(new BrokenOutputSocket(back));
            assertThat(pool.size(), is(1));
            assertThat(new String(front.getData()), equalTo(MOCK_ENTRY_DATA_WRITE));
            assertThat(new String(back.getData()), equalTo(MOCK_ENTRY_DATA_READ));
            assertThat(back.getCount(READ), is(1));
            assertThat(back.getCount(WRITE), is(0));
            assertThat(cache.getEntry(), notNullValue());
            assertThat(cache.getEntry().getSize(DATA), equalTo((long) MOCK_ENTRY_DATA_READ.length()));

            try {
                IOSocket.copy(front.getInputSocket(), cache.getOutputSocket());
                if (WRITE_THROUGH != strategy) {
                    assertThat( back.getCount(WRITE), is(0));
                    cache.flush();
                }
                fail();
            } catch (IOException expected) {
            }
            assertThat(pool.size(), is(1));
            assertThat(new String(front.getData()), equalTo(MOCK_ENTRY_DATA_WRITE));
            assertThat(new String(back.getData()), equalTo(MOCK_ENTRY_DATA_READ));
            assertThat(back.getCount(READ), is(1));
            assertThat(back.getCount(WRITE), is(0));
            assertThat(cache.getEntry(), notNullValue());
            assertThat(cache.getEntry().getSize(DATA), equalTo((long) MOCK_ENTRY_DATA_WRITE.length()));

            cache   .configure(back.getInputSocket())
                    .configure(back.getOutputSocket());
            assertThat(pool.size(), is(1));
            assertThat(new String(front.getData()), equalTo(MOCK_ENTRY_DATA_WRITE));
            assertThat(new String(back.getData()), equalTo(MOCK_ENTRY_DATA_READ));
            assertThat(back.getCount(READ), is(1));
            assertThat(back.getCount(WRITE), is(0));
            assertThat(cache.getEntry(), notNullValue());
            assertThat(cache.getEntry().getSize(DATA), equalTo((long) MOCK_ENTRY_DATA_WRITE.length()));

            IOSocket.copy(front.getInputSocket(), cache.getOutputSocket());
            if (WRITE_THROUGH != strategy) {
                assertThat( back.getCount(WRITE), is(0));
                cache.flush();
            }
            assertThat(cache.getEntry(), notNullValue());
            assertThat(pool.size(), is(1));
            assertThat(new String(front.getData()), equalTo(MOCK_ENTRY_DATA_WRITE));
            assertThat(new String(back.getData()), equalTo(MOCK_ENTRY_DATA_WRITE));
            assertThat(back.getCount(READ), is(1));
            assertThat(back.getCount(WRITE), is(1));
            assertThat(cache.getEntry(), notNullValue());
            assertThat(cache.getEntry().getSize(DATA), equalTo((long) MOCK_ENTRY_DATA_WRITE.length()));

            back = new ByteArrayIOBuffer(MOCK_ENTRY_NAME, INITIAL_CAPACITY);
            back.setData(MOCK_ENTRY_DATA_READ.getBytes());
            cache   .configure(new BrokenInputSocket(back))
                    .configure(new BrokenOutputSocket(back));
            assertThat(cache.getEntry(), notNullValue());
            assertThat(pool.size(), is(1));
            assertThat(new String(front.getData()), equalTo(MOCK_ENTRY_DATA_WRITE));
            assertThat(new String(back.getData()), equalTo(MOCK_ENTRY_DATA_READ));
            assertThat(back.getCount(READ), is(0));
            assertThat(back.getCount(WRITE), is(0));
            assertThat(cache.getEntry(), notNullValue());
            assertThat(cache.getEntry().getSize(DATA), equalTo((long) MOCK_ENTRY_DATA_WRITE.length()));

            front = new ByteArrayIOBuffer(MOCK_ENTRY_NAME, INITIAL_CAPACITY);
            IOSocket.copy(cache.getInputSocket(), front.getOutputSocket());
            assertThat(cache.getEntry(), notNullValue());
            assertThat(pool.size(), is(1));
            assertThat(new String(front.getData()), equalTo(MOCK_ENTRY_DATA_WRITE));
            assertThat(new String(back.getData()), equalTo(MOCK_ENTRY_DATA_READ));
            assertThat(back.getCount(READ), is(0));
            assertThat(back.getCount(WRITE), is(0));
            assertThat(cache.getEntry(), notNullValue());
            assertThat(cache.getEntry().getSize(DATA), equalTo((long) MOCK_ENTRY_DATA_WRITE.length()));

            cache.clear();
            assertThat(cache.getEntry(), nullValue());
            assertThat(pool.size(), is(0));
            assertThat(new String(front.getData()), equalTo(MOCK_ENTRY_DATA_WRITE));
            assertThat(new String(back.getData()), equalTo(MOCK_ENTRY_DATA_READ));
            assertThat(back.getCount(READ), is(0));
            assertThat(back.getCount(WRITE), is(0));
            assertThat(cache.getEntry(), nullValue());

            front = new ByteArrayIOBuffer(MOCK_ENTRY_NAME, INITIAL_CAPACITY);
            try {
                IOSocket.copy(cache.getInputSocket(), front.getOutputSocket());
                fail();
            } catch (IOException excepted) {
            }
            assertThat(cache.getEntry(), nullValue());
            assertThat(pool.size(), is(0));
            assertThat(front.getData(), nullValue());
            assertThat(new String(back.getData()), equalTo(MOCK_ENTRY_DATA_READ));
            assertThat(back.getCount(READ), is(0));
            assertThat(back.getCount(WRITE), is(0));
            assertThat(cache.getEntry(), nullValue());

            cache   .configure(back.getInputSocket())
                    .configure(back.getOutputSocket());
            assertThat(cache.getEntry(), nullValue());
            assertThat(pool.size(), is(0));
            assertThat(front.getData(), nullValue());
            assertThat(new String(back.getData()), equalTo(MOCK_ENTRY_DATA_READ));
            assertThat(back.getCount(READ), is(0));
            assertThat(back.getCount(WRITE), is(0));
            assertThat(cache.getEntry(), nullValue());

            IOSocket.copy(cache.getInputSocket(), front.getOutputSocket());
            assertThat(cache.getEntry(), notNullValue());
            assertThat(pool.size(), is(1));
            assertThat(new String(front.getData()), equalTo(MOCK_ENTRY_DATA_READ));
            assertThat(new String(back.getData()), equalTo(MOCK_ENTRY_DATA_READ));
            assertThat(back.getCount(READ), is(1));
            assertThat(back.getCount(WRITE), is(0));
            assertThat(cache.getEntry(), notNullValue());
            assertThat(cache.getEntry().getSize(DATA), equalTo((long) MOCK_ENTRY_DATA_READ.length()));

            front = new ByteArrayIOBuffer(MOCK_ENTRY_NAME, INITIAL_CAPACITY);
            front.setData(MOCK_ENTRY_DATA_WRITE.getBytes());
            cache   .configure(new BrokenInputSocket(back))
                    .configure(new BrokenOutputSocket(back));
            assertThat(cache.getEntry(), notNullValue());
            assertThat(pool.size(), is(1));
            assertThat(new String(front.getData()), equalTo(MOCK_ENTRY_DATA_WRITE));
            assertThat(new String(back.getData()), equalTo(MOCK_ENTRY_DATA_READ));
            assertThat(back.getCount(READ), is(1));
            assertThat(back.getCount(WRITE), is(0));
            assertThat(cache.getEntry(), notNullValue());
            assertThat(cache.getEntry().getSize(DATA), equalTo((long) MOCK_ENTRY_DATA_READ.length()));

            try {
                IOSocket.copy(front.getInputSocket(), cache.getOutputSocket());
                if (WRITE_THROUGH != strategy) {
                    assertThat( back.getCount(WRITE), is(0));
                    cache.flush();
                }
                fail();
            } catch (IOException expected) {
            }
            assertThat(cache.getEntry(), notNullValue());
            assertThat(pool.size(), is(1));
            assertThat(new String(front.getData()), equalTo(MOCK_ENTRY_DATA_WRITE));
            assertThat(new String(back.getData()), equalTo(MOCK_ENTRY_DATA_READ));
            assertThat(back.getCount(READ), is(1));
            assertThat(back.getCount(WRITE), is(0));
            assertThat(cache.getEntry(), notNullValue());
            assertThat(cache.getEntry().getSize(DATA), equalTo((long) MOCK_ENTRY_DATA_WRITE.length()));

            cache   .configure(back.getInputSocket())
                    .configure(back.getOutputSocket());
            assertThat(cache.getEntry(), notNullValue());
            assertThat(pool.size(), is(1));
            assertThat(new String(front.getData()), equalTo(MOCK_ENTRY_DATA_WRITE));
            assertThat(new String(back.getData()), equalTo(MOCK_ENTRY_DATA_READ));
            assertThat(back.getCount(READ), is(1));
            assertThat(back.getCount(WRITE), is(0));
            assertThat(cache.getEntry(), notNullValue());
            assertThat(cache.getEntry().getSize(DATA), equalTo((long) MOCK_ENTRY_DATA_WRITE.length()));

            IOSocket.copy(front.getInputSocket(), cache.getOutputSocket());
            if (WRITE_THROUGH != strategy) {
                assertThat( back.getCount(WRITE), is(0));
                cache.flush();
            }
            assertThat(cache.getEntry(), notNullValue());
            assertThat(pool.size(), is(1));
            assertThat(new String(front.getData()), equalTo(MOCK_ENTRY_DATA_WRITE));
            assertThat(new String(back.getData()), equalTo(MOCK_ENTRY_DATA_WRITE));
            assertThat(back.getCount(READ), is(1));
            assertThat(back.getCount(WRITE), is(1));
            assertThat(cache.getEntry(), notNullValue());
            assertThat(cache.getEntry().getSize(DATA), equalTo((long) MOCK_ENTRY_DATA_WRITE.length()));

            cache   .configure(new BrokenInputSocket(back))
                    .configure(new BrokenOutputSocket(back))
                    .clear();
            assertThat(cache.getEntry(), nullValue());
            assertThat(pool.size(), is(0));
            assertThat(new String(front.getData()), equalTo(MOCK_ENTRY_DATA_WRITE));
            assertThat(new String(back.getData()), equalTo(MOCK_ENTRY_DATA_WRITE));
            assertThat(back.getCount(READ), is(1));
            assertThat(back.getCount(WRITE), is(1));
            assertThat(cache.getEntry(), nullValue());
        }
    }

    private static class BrokenInputSocket
    extends InputSocket<Entry> {
        private final Entry entry;

        BrokenInputSocket(Entry entry) {
            if (null == entry)
                throw new NullPointerException();
            this.entry = entry;
        }

        @Override
        public Entry getLocalTarget() throws IOException {
            return entry;
        }

        @Override
        public ReadOnlyFile newReadOnlyFile() throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public InputStream newInputStream() throws IOException {
            return new BrokenInputStream();
        }

        static class BrokenInputStream extends InputStream {

            @Override
            public int read() throws IOException {
                throw new IOException();
            }
        } // class BrokenInputStream
    } // class BrokenInputSocket

    private static class BrokenOutputSocket
    extends OutputSocket<Entry> {
        private final Entry entry;

        BrokenOutputSocket(Entry entry) {
            if (null == entry)
                throw new NullPointerException();
            this.entry = entry;
        }

        @Override
        public Entry getLocalTarget() throws IOException {
            return entry;
        }

        @Override
        public OutputStream newOutputStream() throws IOException {
            return new BrokenOutputStream();
        }

        static class BrokenOutputStream extends OutputStream {

            @Override
            public void write(int b) throws IOException {
                throw new IOException();
            }
        } // class BrokenOutputStream
    } // class BrokenOutputSocket
}