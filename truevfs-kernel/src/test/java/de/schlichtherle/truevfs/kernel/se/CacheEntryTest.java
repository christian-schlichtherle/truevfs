/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truevfs.kernel.se;

import de.schlichtherle.truevfs.kernel.se.CacheEntry.Strategy;
import static de.schlichtherle.truevfs.kernel.se.CacheEntry.Strategy.WRITE_BACK;
import static de.schlichtherle.truevfs.kernel.se.CacheEntry.Strategy.WRITE_THROUGH;
import static net.truevfs.kernel.cio.Entry.Access.READ;
import static net.truevfs.kernel.cio.Entry.Access.WRITE;
import static net.truevfs.kernel.cio.Entry.Size.DATA;
import static net.truevfs.kernel.cio.Entry.UNKNOWN;
import net.truevfs.kernel.cio.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Christian Schlichtherle
 */
public final class CacheEntryTest {

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
            final CacheEntry cache = strategy.newCacheEntry(pool);
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
            assertThat(cache.getSize(DATA), is((long) UNKNOWN));

            front = new ByteArrayIOBuffer(MOCK_ENTRY_NAME, INITIAL_CAPACITY);
            assertThat(front.getData(), nullValue());
            try {
                IOSockets.copy(cache.input(), front.output());
                fail();
            } catch (IOException expected) {
            }
            assertThat(pool.size(), is(0));
            assertThat(front.getData(), nullValue());
            assertThat(new String(back.getData()), equalTo(MOCK_ENTRY_DATA_READ));
            assertThat(back.getCount(READ), is(0));
            assertThat(back.getCount(WRITE), is(0));
            assertThat(cache.getSize(DATA), is((long) UNKNOWN));

            cache   .configure(back.input())
                    .configure(back.output());
            assertThat(pool.size(), is(0));
            assertThat(front.getData(), nullValue());
            assertThat(new String(back.getData()), equalTo(MOCK_ENTRY_DATA_READ));
            assertThat(back.getCount(READ), is(0));
            assertThat(back.getCount(WRITE), is(0));
            assertThat(cache.getSize(DATA), is((long) UNKNOWN));

            front = new ByteArrayIOBuffer(MOCK_ENTRY_NAME, INITIAL_CAPACITY);
            assertThat(pool.size(), is(0));
            assertThat(front.getData(), nullValue());
            IOSockets.copy(cache.input(), front.output());
            assertThat(pool.size(), is(1));
            assertThat(new String(front.getData()), equalTo(MOCK_ENTRY_DATA_READ));
            assertThat(new String(back.getData()), equalTo(MOCK_ENTRY_DATA_READ));
            assertThat(back.getCount(READ), is(1));
            assertThat(back.getCount(WRITE), is(0));
            assertThat(cache.getSize(DATA), is((long) MOCK_ENTRY_DATA_READ.length()));

            front = new ByteArrayIOBuffer(MOCK_ENTRY_NAME, INITIAL_CAPACITY);
            front.setData(MOCK_ENTRY_DATA_WRITE.getBytes());
            cache   .configure(new BrokenInputSocket(back))
                    .configure(new BrokenOutputSocket(back));
            assertThat(pool.size(), is(1));
            assertThat(new String(front.getData()), equalTo(MOCK_ENTRY_DATA_WRITE));
            assertThat(new String(back.getData()), equalTo(MOCK_ENTRY_DATA_READ));
            assertThat(back.getCount(READ), is(1));
            assertThat(back.getCount(WRITE), is(0));
            assertThat(cache.getSize(DATA), is((long) MOCK_ENTRY_DATA_READ.length()));

            try {
                IOSockets.copy(front.input(), cache.output());
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
            assertThat(cache.getSize(DATA), is((long) MOCK_ENTRY_DATA_WRITE.length()));

            cache   .configure(back.input())
                    .configure(back.output());
            assertThat(pool.size(), is(1));
            assertThat(new String(front.getData()), equalTo(MOCK_ENTRY_DATA_WRITE));
            assertThat(new String(back.getData()), equalTo(MOCK_ENTRY_DATA_READ));
            assertThat(back.getCount(READ), is(1));
            assertThat(back.getCount(WRITE), is(0));
            assertThat(cache.getSize(DATA), is((long) MOCK_ENTRY_DATA_WRITE.length()));

            IOSockets.copy(front.input(), cache.output());
            if (WRITE_THROUGH != strategy) {
                assertThat( back.getCount(WRITE), is(0));
                cache.flush();
            }
            assertThat(cache.getSize(DATA), is(not((long) UNKNOWN)));
            assertThat(pool.size(), is(1));
            assertThat(new String(front.getData()), equalTo(MOCK_ENTRY_DATA_WRITE));
            assertThat(new String(back.getData()), equalTo(MOCK_ENTRY_DATA_WRITE));
            assertThat(back.getCount(READ), is(1));
            assertThat(back.getCount(WRITE), is(1));
            assertThat(cache.getSize(DATA), is((long) MOCK_ENTRY_DATA_WRITE.length()));

            back = new ByteArrayIOBuffer(MOCK_ENTRY_NAME, INITIAL_CAPACITY);
            back.setData(MOCK_ENTRY_DATA_READ.getBytes());
            cache   .configure(new BrokenInputSocket(back))
                    .configure(new BrokenOutputSocket(back));
            assertThat(cache.getSize(DATA), is(not((long) UNKNOWN)));
            assertThat(pool.size(), is(1));
            assertThat(new String(front.getData()), equalTo(MOCK_ENTRY_DATA_WRITE));
            assertThat(new String(back.getData()), equalTo(MOCK_ENTRY_DATA_READ));
            assertThat(back.getCount(READ), is(0));
            assertThat(back.getCount(WRITE), is(0));
            assertThat(cache.getSize(DATA), is((long) MOCK_ENTRY_DATA_WRITE.length()));

            front = new ByteArrayIOBuffer(MOCK_ENTRY_NAME, INITIAL_CAPACITY);
            IOSockets.copy(cache.input(), front.output());
            assertThat(cache.getSize(DATA), is(not((long) UNKNOWN)));
            assertThat(pool.size(), is(1));
            assertThat(new String(front.getData()), equalTo(MOCK_ENTRY_DATA_WRITE));
            assertThat(new String(back.getData()), equalTo(MOCK_ENTRY_DATA_READ));
            assertThat(back.getCount(READ), is(0));
            assertThat(back.getCount(WRITE), is(0));
            assertThat(cache.getSize(DATA), is((long) MOCK_ENTRY_DATA_WRITE.length()));

            cache.release();
            assertThat(cache.getSize(DATA), is((long) UNKNOWN));
            assertThat(pool.size(), is(0));
            assertThat(new String(front.getData()), equalTo(MOCK_ENTRY_DATA_WRITE));
            assertThat(new String(back.getData()), equalTo(MOCK_ENTRY_DATA_READ));
            assertThat(back.getCount(READ), is(0));
            assertThat(back.getCount(WRITE), is(0));
            assertThat(cache.getSize(DATA), is((long) UNKNOWN));

            front = new ByteArrayIOBuffer(MOCK_ENTRY_NAME, INITIAL_CAPACITY);
            try {
                IOSockets.copy(cache.input(), front.output());
                fail();
            } catch (IOException excepted) {
            }
            assertThat(cache.getSize(DATA), is((long) UNKNOWN));
            assertThat(pool.size(), is(0));
            assertThat(front.getData(), nullValue());
            assertThat(new String(back.getData()), equalTo(MOCK_ENTRY_DATA_READ));
            assertThat(back.getCount(READ), is(0));
            assertThat(back.getCount(WRITE), is(0));
            assertThat(cache.getSize(DATA), is((long) UNKNOWN));

            cache   .configure(back.input())
                    .configure(back.output());
            assertThat(cache.getSize(DATA), is((long) UNKNOWN));
            assertThat(pool.size(), is(0));
            assertThat(front.getData(), nullValue());
            assertThat(new String(back.getData()), equalTo(MOCK_ENTRY_DATA_READ));
            assertThat(back.getCount(READ), is(0));
            assertThat(back.getCount(WRITE), is(0));
            assertThat(cache.getSize(DATA), is((long) UNKNOWN));

            IOSockets.copy(cache.input(), front.output());
            assertThat(cache.getSize(DATA), is(not((long) UNKNOWN)));
            assertThat(pool.size(), is(1));
            assertThat(new String(front.getData()), equalTo(MOCK_ENTRY_DATA_READ));
            assertThat(new String(back.getData()), equalTo(MOCK_ENTRY_DATA_READ));
            assertThat(back.getCount(READ), is(1));
            assertThat(back.getCount(WRITE), is(0));
            assertThat(cache.getSize(DATA), is((long) MOCK_ENTRY_DATA_READ.length()));

            front = new ByteArrayIOBuffer(MOCK_ENTRY_NAME, INITIAL_CAPACITY);
            front.setData(MOCK_ENTRY_DATA_WRITE.getBytes());
            cache   .configure(new BrokenInputSocket(back))
                    .configure(new BrokenOutputSocket(back));
            assertThat(cache.getSize(DATA), is(not((long) UNKNOWN)));
            assertThat(pool.size(), is(1));
            assertThat(new String(front.getData()), equalTo(MOCK_ENTRY_DATA_WRITE));
            assertThat(new String(back.getData()), equalTo(MOCK_ENTRY_DATA_READ));
            assertThat(back.getCount(READ), is(1));
            assertThat(back.getCount(WRITE), is(0));
            assertThat(cache.getSize(DATA), is((long) MOCK_ENTRY_DATA_READ.length()));

            try {
                IOSockets.copy(front.input(), cache.output());
                if (WRITE_THROUGH != strategy) {
                    assertThat( back.getCount(WRITE), is(0));
                    cache.flush();
                }
                fail();
            } catch (IOException expected) {
            }
            assertThat(cache.getSize(DATA), is(not((long) UNKNOWN)));
            assertThat(pool.size(), is(1));
            assertThat(new String(front.getData()), equalTo(MOCK_ENTRY_DATA_WRITE));
            assertThat(new String(back.getData()), equalTo(MOCK_ENTRY_DATA_READ));
            assertThat(back.getCount(READ), is(1));
            assertThat(back.getCount(WRITE), is(0));
            assertThat(cache.getSize(DATA), is((long) MOCK_ENTRY_DATA_WRITE.length()));

            cache   .configure(back.input())
                    .configure(back.output());
            assertThat(cache.getSize(DATA), is(not((long) UNKNOWN)));
            assertThat(pool.size(), is(1));
            assertThat(new String(front.getData()), equalTo(MOCK_ENTRY_DATA_WRITE));
            assertThat(new String(back.getData()), equalTo(MOCK_ENTRY_DATA_READ));
            assertThat(back.getCount(READ), is(1));
            assertThat(back.getCount(WRITE), is(0));
            assertThat(cache.getSize(DATA), is((long) MOCK_ENTRY_DATA_WRITE.length()));

            IOSockets.copy(front.input(), cache.output());
            if (WRITE_THROUGH != strategy) {
                assertThat( back.getCount(WRITE), is(0));
                cache.flush();
            }
            assertThat(cache.getSize(DATA), is(not((long) UNKNOWN)));
            assertThat(pool.size(), is(1));
            assertThat(new String(front.getData()), equalTo(MOCK_ENTRY_DATA_WRITE));
            assertThat(new String(back.getData()), equalTo(MOCK_ENTRY_DATA_WRITE));
            assertThat(back.getCount(READ), is(1));
            assertThat(back.getCount(WRITE), is(1));
            assertThat(cache.getSize(DATA), is((long) MOCK_ENTRY_DATA_WRITE.length()));

            cache   .configure(new BrokenInputSocket(back))
                    .configure(new BrokenOutputSocket(back))
                    .release();
            assertThat(cache.getSize(DATA), is((long) UNKNOWN));
            assertThat(pool.size(), is(0));
            assertThat(new String(front.getData()), equalTo(MOCK_ENTRY_DATA_WRITE));
            assertThat(new String(back.getData()), equalTo(MOCK_ENTRY_DATA_WRITE));
            assertThat(back.getCount(READ), is(1));
            assertThat(back.getCount(WRITE), is(1));
            assertThat(cache.getSize(DATA), is((long) UNKNOWN));
        }
    }

    private static class BrokenInputSocket
    extends AbstractInputSocket<Entry> {
        private final Entry entry;

        BrokenInputSocket(Entry entry) {
            this.entry = Objects.requireNonNull(entry);
        }

        @Override
        public Entry localTarget() throws IOException {
            return entry;
        }

        @Override
        public InputStream stream() throws IOException {
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
    extends AbstractOutputSocket<Entry> {
        private final Entry entry;

        BrokenOutputSocket(Entry entry) {
            this.entry = Objects.requireNonNull(entry);
        }

        @Override
        public Entry localTarget() throws IOException {
            return entry;
        }

        @Override
        public OutputStream stream() throws IOException {
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