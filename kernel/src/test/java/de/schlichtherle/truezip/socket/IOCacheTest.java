/*
 * Copyright 2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.socket;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import java.io.InputStream;
import java.io.OutputStream;
import de.schlichtherle.truezip.socket.IOCache.Strategy;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;

import static de.schlichtherle.truezip.entry.Entry.Size.*;
import static de.schlichtherle.truezip.socket.IOCache.Strategy.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public final class IOCacheTest {

    private static final String MOCK_ENTRY_NAME = "mock";
    private static final String MOCK_ENTRY_DATA_READ = "read";
    private static final String MOCK_ENTRY_DATA_WRITE = "write";

    private ByteArrayIOPool pool;

    @Before
    public final void setUp() throws IOException {
        pool = new ByteArrayIOPool(5);
    }

    @Test
    public void testCaching() throws IOException {
        for (final Strategy strategy : new Strategy[] {
            WRITE_THROUGH,
            WRITE_BACK,
        }) {
            final IOCache cache = strategy.newCache(pool);
            ByteArrayIOEntry front;
            ByteArrayIOEntry back;

            back = new ByteArrayIOEntry(MOCK_ENTRY_NAME);
            back.setData(MOCK_ENTRY_DATA_READ.getBytes());
            cache   .configure(new BrokenInputSocket(back))
                    .configure(new BrokenOutputSocket(back));
            assertThat(pool.getSize(), is(0));
            assertThat(new String(back.getData()), equalTo(MOCK_ENTRY_DATA_READ));
            assertThat(back.getReads(), is(0));
            assertThat(back.getWrites(), is(0));
            assertThat(cache.getEntry(), nullValue());

            front = new ByteArrayIOEntry(MOCK_ENTRY_NAME);
            assertThat(front.getData(), nullValue());
            try {
                IOSocket.copy(cache.getInputSocket(), front.getOutputSocket());
                fail();
            } catch (IOException expected) {
            }
            assertThat(pool.getSize(), is(0));
            assertThat(front.getData(), nullValue());
            assertThat(new String(back.getData()), equalTo(MOCK_ENTRY_DATA_READ));
            assertThat(back.getReads(), is(0));
            assertThat(back.getWrites(), is(0));
            assertThat(cache.getEntry(), nullValue());

            cache   .configure(back.getInputSocket())
                    .configure(back.getOutputSocket());
            assertThat(pool.getSize(), is(0));
            assertThat(front.getData(), nullValue());
            assertThat(new String(back.getData()), equalTo(MOCK_ENTRY_DATA_READ));
            assertThat(back.getReads(), is(0));
            assertThat(back.getWrites(), is(0));
            assertThat(cache.getEntry(), nullValue());

            front = new ByteArrayIOEntry(MOCK_ENTRY_NAME);
            assertThat(pool.getSize(), is(0));
            assertThat(front.getData(), nullValue());
            IOSocket.copy(cache.getInputSocket(), front.getOutputSocket());
            assertThat(pool.getSize(), is(1));
            assertThat(new String(front.getData()), equalTo(MOCK_ENTRY_DATA_READ));
            assertThat(new String(back.getData()), equalTo(MOCK_ENTRY_DATA_READ));
            assertThat(back.getReads(), is(1));
            assertThat(back.getWrites(), is(0));
            assertThat(cache.getEntry(), notNullValue());
            assertThat(cache.getEntry().getSize(DATA), equalTo((long) MOCK_ENTRY_DATA_READ.length()));

            front = new ByteArrayIOEntry(MOCK_ENTRY_NAME);
            front.setData(MOCK_ENTRY_DATA_WRITE.getBytes());
            cache   .configure(new BrokenInputSocket(back))
                    .configure(new BrokenOutputSocket(back));
            assertThat(pool.getSize(), is(1));
            assertThat(new String(front.getData()), equalTo(MOCK_ENTRY_DATA_WRITE));
            assertThat(new String(back.getData()), equalTo(MOCK_ENTRY_DATA_READ));
            assertThat(back.getReads(), is(1));
            assertThat(back.getWrites(), is(0));
            assertThat(cache.getEntry(), notNullValue());
            assertThat(cache.getEntry().getSize(DATA), equalTo((long) MOCK_ENTRY_DATA_READ.length()));

            try {
                IOSocket.copy(front.getInputSocket(), cache.getOutputSocket());
                if (WRITE_THROUGH != strategy) {
                    assertThat( back.getWrites(), is(0));
                    cache.flush();
                }
                fail();
            } catch (IOException expected) {
            }
            assertThat(pool.getSize(), is(1));
            assertThat(new String(front.getData()), equalTo(MOCK_ENTRY_DATA_WRITE));
            assertThat(new String(back.getData()), equalTo(MOCK_ENTRY_DATA_READ));
            assertThat(back.getReads(), is(1));
            assertThat(back.getWrites(), is(0));
            assertThat(cache.getEntry(), notNullValue());
            assertThat(cache.getEntry().getSize(DATA), equalTo((long) MOCK_ENTRY_DATA_WRITE.length()));

            cache   .configure(back.getInputSocket())
                    .configure(back.getOutputSocket());
            assertThat(pool.getSize(), is(1));
            assertThat(new String(front.getData()), equalTo(MOCK_ENTRY_DATA_WRITE));
            assertThat(new String(back.getData()), equalTo(MOCK_ENTRY_DATA_READ));
            assertThat(back.getReads(), is(1));
            assertThat(back.getWrites(), is(0));
            assertThat(cache.getEntry(), notNullValue());
            assertThat(cache.getEntry().getSize(DATA), equalTo((long) MOCK_ENTRY_DATA_WRITE.length()));

            IOSocket.copy(front.getInputSocket(), cache.getOutputSocket());
            if (WRITE_THROUGH != strategy) {
                assertThat( back.getWrites(), is(0));
                cache.flush();
            }
            assertThat(cache.getEntry(), notNullValue());
            assertThat(pool.getSize(), is(1));
            assertThat(new String(front.getData()), equalTo(MOCK_ENTRY_DATA_WRITE));
            assertThat(new String(back.getData()), equalTo(MOCK_ENTRY_DATA_WRITE));
            assertThat(back.getReads(), is(1));
            assertThat(back.getWrites(), is(1));
            assertThat(cache.getEntry(), notNullValue());
            assertThat(cache.getEntry().getSize(DATA), equalTo((long) MOCK_ENTRY_DATA_WRITE.length()));

            back = new ByteArrayIOEntry(MOCK_ENTRY_NAME);
            back.setData(MOCK_ENTRY_DATA_READ.getBytes());
            cache   .configure(new BrokenInputSocket(back))
                    .configure(new BrokenOutputSocket(back));
            assertThat(cache.getEntry(), notNullValue());
            assertThat(pool.getSize(), is(1));
            assertThat(new String(front.getData()), equalTo(MOCK_ENTRY_DATA_WRITE));
            assertThat(new String(back.getData()), equalTo(MOCK_ENTRY_DATA_READ));
            assertThat(back.getReads(), is(0));
            assertThat(back.getWrites(), is(0));
            assertThat(cache.getEntry(), notNullValue());
            assertThat(cache.getEntry().getSize(DATA), equalTo((long) MOCK_ENTRY_DATA_WRITE.length()));

            front = new ByteArrayIOEntry(MOCK_ENTRY_NAME);
            IOSocket.copy(cache.getInputSocket(), front.getOutputSocket());
            assertThat(cache.getEntry(), notNullValue());
            assertThat(pool.getSize(), is(1));
            assertThat(new String(front.getData()), equalTo(MOCK_ENTRY_DATA_WRITE));
            assertThat(new String(back.getData()), equalTo(MOCK_ENTRY_DATA_READ));
            assertThat(back.getReads(), is(0));
            assertThat(back.getWrites(), is(0));
            assertThat(cache.getEntry(), notNullValue());
            assertThat(cache.getEntry().getSize(DATA), equalTo((long) MOCK_ENTRY_DATA_WRITE.length()));

            cache.clear();
            assertThat(cache.getEntry(), nullValue());
            assertThat(pool.getSize(), is(0));
            assertThat(new String(front.getData()), equalTo(MOCK_ENTRY_DATA_WRITE));
            assertThat(new String(back.getData()), equalTo(MOCK_ENTRY_DATA_READ));
            assertThat(back.getReads(), is(0));
            assertThat(back.getWrites(), is(0));
            assertThat(cache.getEntry(), nullValue());

            front = new ByteArrayIOEntry(MOCK_ENTRY_NAME);
            try {
                IOSocket.copy(cache.getInputSocket(), front.getOutputSocket());
                fail();
            } catch (IOException excepted) {
            }
            assertThat(cache.getEntry(), nullValue());
            assertThat(pool.getSize(), is(0));
            assertThat(front.getData(), nullValue());
            assertThat(new String(back.getData()), equalTo(MOCK_ENTRY_DATA_READ));
            assertThat(back.getReads(), is(0));
            assertThat(back.getWrites(), is(0));
            assertThat(cache.getEntry(), nullValue());

            cache   .configure(back.getInputSocket())
                    .configure(back.getOutputSocket());
            assertThat(cache.getEntry(), nullValue());
            assertThat(pool.getSize(), is(0));
            assertThat(front.getData(), nullValue());
            assertThat(new String(back.getData()), equalTo(MOCK_ENTRY_DATA_READ));
            assertThat(back.getReads(), is(0));
            assertThat(back.getWrites(), is(0));
            assertThat(cache.getEntry(), nullValue());

            IOSocket.copy(cache.getInputSocket(), front.getOutputSocket());
            assertThat(cache.getEntry(), notNullValue());
            assertThat(pool.getSize(), is(1));
            assertThat(new String(front.getData()), equalTo(MOCK_ENTRY_DATA_READ));
            assertThat(new String(back.getData()), equalTo(MOCK_ENTRY_DATA_READ));
            assertThat(back.getReads(), is(1));
            assertThat(back.getWrites(), is(0));
            assertThat(cache.getEntry(), notNullValue());
            assertThat(cache.getEntry().getSize(DATA), equalTo((long) MOCK_ENTRY_DATA_READ.length()));

            front = new ByteArrayIOEntry(MOCK_ENTRY_NAME);
            front.setData(MOCK_ENTRY_DATA_WRITE.getBytes());
            cache   .configure(new BrokenInputSocket(back))
                    .configure(new BrokenOutputSocket(back));
            assertThat(cache.getEntry(), notNullValue());
            assertThat(pool.getSize(), is(1));
            assertThat(new String(front.getData()), equalTo(MOCK_ENTRY_DATA_WRITE));
            assertThat(new String(back.getData()), equalTo(MOCK_ENTRY_DATA_READ));
            assertThat(back.getReads(), is(1));
            assertThat(back.getWrites(), is(0));
            assertThat(cache.getEntry(), notNullValue());
            assertThat(cache.getEntry().getSize(DATA), equalTo((long) MOCK_ENTRY_DATA_READ.length()));

            try {
                IOSocket.copy(front.getInputSocket(), cache.getOutputSocket());
                if (WRITE_THROUGH != strategy) {
                    assertThat( back.getWrites(), is(0));
                    cache.flush();
                }
                fail();
            } catch (IOException expected) {
            }
            assertThat(cache.getEntry(), notNullValue());
            assertThat(pool.getSize(), is(1));
            assertThat(new String(front.getData()), equalTo(MOCK_ENTRY_DATA_WRITE));
            assertThat(new String(back.getData()), equalTo(MOCK_ENTRY_DATA_READ));
            assertThat(back.getReads(), is(1));
            assertThat(back.getWrites(), is(0));
            assertThat(cache.getEntry(), notNullValue());
            assertThat(cache.getEntry().getSize(DATA), equalTo((long) MOCK_ENTRY_DATA_WRITE.length()));

            cache   .configure(back.getInputSocket())
                    .configure(back.getOutputSocket());
            assertThat(cache.getEntry(), notNullValue());
            assertThat(pool.getSize(), is(1));
            assertThat(new String(front.getData()), equalTo(MOCK_ENTRY_DATA_WRITE));
            assertThat(new String(back.getData()), equalTo(MOCK_ENTRY_DATA_READ));
            assertThat(back.getReads(), is(1));
            assertThat(back.getWrites(), is(0));
            assertThat(cache.getEntry(), notNullValue());
            assertThat(cache.getEntry().getSize(DATA), equalTo((long) MOCK_ENTRY_DATA_WRITE.length()));

            IOSocket.copy(front.getInputSocket(), cache.getOutputSocket());
            if (WRITE_THROUGH != strategy) {
                assertThat( back.getWrites(), is(0));
                cache.flush();
            }
            assertThat(cache.getEntry(), notNullValue());
            assertThat(pool.getSize(), is(1));
            assertThat(new String(front.getData()), equalTo(MOCK_ENTRY_DATA_WRITE));
            assertThat(new String(back.getData()), equalTo(MOCK_ENTRY_DATA_WRITE));
            assertThat(back.getReads(), is(1));
            assertThat(back.getWrites(), is(1));
            assertThat(cache.getEntry(), notNullValue());
            assertThat(cache.getEntry().getSize(DATA), equalTo((long) MOCK_ENTRY_DATA_WRITE.length()));

            cache   .configure(new BrokenInputSocket(back))
                    .configure(new BrokenOutputSocket(back))
                    .clear();
            assertThat(cache.getEntry(), nullValue());
            assertThat(pool.getSize(), is(0));
            assertThat(new String(front.getData()), equalTo(MOCK_ENTRY_DATA_WRITE));
            assertThat(new String(back.getData()), equalTo(MOCK_ENTRY_DATA_WRITE));
            assertThat(back.getReads(), is(1));
            assertThat(back.getWrites(), is(1));
            assertThat(cache.getEntry(), nullValue());
        }
    }

    private static class BrokenInputSocket
    extends InputSocket<Entry> {
        private final Entry entry;

        public BrokenInputSocket(Entry entry) {
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

        public BrokenOutputSocket(Entry entry) {
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
