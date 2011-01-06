package de.schlichtherle.truezip.io.socket;

import de.schlichtherle.truezip.io.socket.IOCache.Strategy;
import de.schlichtherle.truezip.io.entry.Entry.Access;
import de.schlichtherle.truezip.io.entry.Entry.Size;
import de.schlichtherle.truezip.io.entry.Entry.Type;
import de.schlichtherle.truezip.io.rof.ReadOnlyFile;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;

import static de.schlichtherle.truezip.io.entry.Entry.Size.*;
import static de.schlichtherle.truezip.io.entry.Entry.Type.*;
import static de.schlichtherle.truezip.io.socket.IOCache.Strategy.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

public class IOCacheTest {

    private static final String MOCK_ENTRY_NAME = "mock";
    private static final String MOCK_ENTRY_DATA_READ = "read";
    private static final String MOCK_ENTRY_DATA_WRITE = "write";

    private MockIOPool pool;

    @Before
    public final void setUp() throws IOException {
        pool = new MockIOPool();
    }

    @Test
    public void testCaching() throws IOException {
        for (final Strategy strategy : new Strategy[] {
            WRITE_THROUGH,
            WRITE_BACK,
        }) {
            final IOCache cache = strategy.newCache(pool);
            MockIOEntry front, back;

            back = new MockIOEntry(MOCK_ENTRY_DATA_READ);
            cache   .configure(back.getBrokenInputSocket())
                    .configure(back.getBrokenOutputSocket());
            assertThat(pool.entries.size(), is(0));
            assertThat(back.toString(), equalTo(MOCK_ENTRY_DATA_READ));
            assertThat(back.reads, is(0));
            assertThat(back.writes, is(0));
            assertThat(cache.getEntry(), nullValue());

            front = new MockIOEntry();
            assertThat(front.toString(), nullValue());
            try {
                IOSocket.copy(cache.getInputSocket(), front.getOutputSocket());
                fail();
            } catch (IOException expected) {
            }
            assertThat(pool.entries.size(), is(0));
            assertThat(front.toString(), nullValue());
            assertThat(back.toString(), equalTo(MOCK_ENTRY_DATA_READ));
            assertThat(back.reads, is(0));
            assertThat(back.writes, is(0));
            assertThat(cache.getEntry(), nullValue());

            cache   .configure(back.getInputSocket())
                    .configure(back.getOutputSocket());
            assertThat(pool.entries.size(), is(0));
            assertThat(front.toString(), nullValue());
            assertThat(back.toString(), equalTo(MOCK_ENTRY_DATA_READ));
            assertThat(back.reads, is(0));
            assertThat(back.writes, is(0));
            assertThat(cache.getEntry(), nullValue());

            front = new MockIOEntry();
            assertThat(pool.entries.size(), is(0));
            assertThat(front.toString(), nullValue());
            IOSocket.copy(cache.getInputSocket(), front.getOutputSocket());
            assertThat(pool.entries.size(), is(1));
            assertThat(front.toString(), equalTo(MOCK_ENTRY_DATA_READ));
            assertThat(back.toString(), equalTo(MOCK_ENTRY_DATA_READ));
            assertThat(back.reads, is(1));
            assertThat(back.writes, is(0));
            assertThat(cache.getEntry(), notNullValue());
            assertThat(cache.getEntry().getSize(DATA), equalTo((long) MOCK_ENTRY_DATA_READ.length()));

            front = new MockIOEntry(MOCK_ENTRY_DATA_WRITE);
            cache   .configure(back.getBrokenInputSocket())
                    .configure(back.getBrokenOutputSocket());
            assertThat(pool.entries.size(), is(1));
            assertThat(front.toString(), equalTo(MOCK_ENTRY_DATA_WRITE));
            assertThat(back.toString(), equalTo(MOCK_ENTRY_DATA_READ));
            assertThat(back.reads, is(1));
            assertThat(back.writes, is(0));
            assertThat(cache.getEntry(), notNullValue());
            assertThat(cache.getEntry().getSize(DATA), equalTo((long) MOCK_ENTRY_DATA_READ.length()));

            try {
                IOSocket.copy(front.getInputSocket(), cache.getOutputSocket());
                if (WRITE_THROUGH != strategy) {
                    assertThat(back.writes, is(0));
                    cache.flush();
                }
                fail();
            } catch (IOException expected) {
            }
            assertThat(pool.entries.size(), is(1));
            assertThat(front.toString(), equalTo(MOCK_ENTRY_DATA_WRITE));
            assertThat(back.toString(), equalTo(MOCK_ENTRY_DATA_READ));
            assertThat(back.reads, is(1));
            assertThat(back.writes, is(0));
            assertThat(cache.getEntry(), notNullValue());
            assertThat(cache.getEntry().getSize(DATA), equalTo((long) MOCK_ENTRY_DATA_WRITE.length()));

            cache   .configure(back.getInputSocket())
                    .configure(back.getOutputSocket());
            assertThat(pool.entries.size(), is(1));
            assertThat(front.toString(), equalTo(MOCK_ENTRY_DATA_WRITE));
            assertThat(back.toString(), equalTo(MOCK_ENTRY_DATA_READ));
            assertThat(back.reads, is(1));
            assertThat(back.writes, is(0));
            assertThat(cache.getEntry(), notNullValue());
            assertThat(cache.getEntry().getSize(DATA), equalTo((long) MOCK_ENTRY_DATA_WRITE.length()));

            IOSocket.copy(front.getInputSocket(), cache.getOutputSocket());
            if (WRITE_THROUGH != strategy) {
                assertThat(back.writes, is(0));
                cache.flush();
            }
            assertThat(cache.getEntry(), notNullValue());
            assertThat(pool.entries.size(), is(1));
            assertThat(front.toString(), equalTo(MOCK_ENTRY_DATA_WRITE));
            assertThat(back.toString(), equalTo(MOCK_ENTRY_DATA_WRITE));
            assertThat(back.reads, is(1));
            assertThat(back.writes, is(1));
            assertThat(cache.getEntry(), notNullValue());
            assertThat(cache.getEntry().getSize(DATA), equalTo((long) MOCK_ENTRY_DATA_WRITE.length()));

            back = new MockIOEntry(MOCK_ENTRY_DATA_READ);
            cache   .configure(back.getBrokenInputSocket())
                    .configure(back.getBrokenOutputSocket());
            assertThat(cache.getEntry(), notNullValue());
            assertThat(pool.entries.size(), is(1));
            assertThat(front.toString(), equalTo(MOCK_ENTRY_DATA_WRITE));
            assertThat(back.toString(), equalTo(MOCK_ENTRY_DATA_READ));
            assertThat(back.reads, is(0));
            assertThat(back.writes, is(0));
            assertThat(cache.getEntry(), notNullValue());
            assertThat(cache.getEntry().getSize(DATA), equalTo((long) MOCK_ENTRY_DATA_WRITE.length()));

            front = new MockIOEntry();
            IOSocket.copy(cache.getInputSocket(), front.getOutputSocket());
            assertThat(cache.getEntry(), notNullValue());
            assertThat(pool.entries.size(), is(1));
            assertThat(front.toString(), equalTo(MOCK_ENTRY_DATA_WRITE));
            assertThat(back.toString(), equalTo(MOCK_ENTRY_DATA_READ));
            assertThat(back.reads, is(0));
            assertThat(back.writes, is(0));
            assertThat(cache.getEntry(), notNullValue());
            assertThat(cache.getEntry().getSize(DATA), equalTo((long) MOCK_ENTRY_DATA_WRITE.length()));

            cache.clear();
            assertThat(cache.getEntry(), nullValue());
            assertThat(pool.entries.size(), is(0));
            assertThat(front.toString(), equalTo(MOCK_ENTRY_DATA_WRITE));
            assertThat(back.toString(), equalTo(MOCK_ENTRY_DATA_READ));
            assertThat(back.reads, is(0));
            assertThat(back.writes, is(0));
            assertThat(cache.getEntry(), nullValue());

            front = new MockIOEntry();
            try {
                IOSocket.copy(cache.getInputSocket(), front.getOutputSocket());
                fail();
            } catch (IOException excepted) {
            }
            assertThat(cache.getEntry(), nullValue());
            assertThat(pool.entries.size(), is(0));
            assertThat(front.toString(), nullValue());
            assertThat(back.toString(), equalTo(MOCK_ENTRY_DATA_READ));
            assertThat(back.reads, is(0));
            assertThat(back.writes, is(0));
            assertThat(cache.getEntry(), nullValue());

            cache   .configure(back.getInputSocket())
                    .configure(back.getOutputSocket());
            assertThat(cache.getEntry(), nullValue());
            assertThat(pool.entries.size(), is(0));
            assertThat(front.toString(), nullValue());
            assertThat(back.toString(), equalTo(MOCK_ENTRY_DATA_READ));
            assertThat(back.reads, is(0));
            assertThat(back.writes, is(0));
            assertThat(cache.getEntry(), nullValue());

            IOSocket.copy(cache.getInputSocket(), front.getOutputSocket());
            assertThat(cache.getEntry(), notNullValue());
            assertThat(pool.entries.size(), is(1));
            assertThat(front.toString(), equalTo(MOCK_ENTRY_DATA_READ));
            assertThat(back.toString(), equalTo(MOCK_ENTRY_DATA_READ));
            assertThat(back.reads, is(1));
            assertThat(back.writes, is(0));
            assertThat(cache.getEntry(), notNullValue());
            assertThat(cache.getEntry().getSize(DATA), equalTo((long) MOCK_ENTRY_DATA_READ.length()));

            front = new MockIOEntry(MOCK_ENTRY_DATA_WRITE);
            cache   .configure(back.getBrokenInputSocket())
                    .configure(back.getBrokenOutputSocket());
            assertThat(cache.getEntry(), notNullValue());
            assertThat(pool.entries.size(), is(1));
            assertThat(front.toString(), equalTo(MOCK_ENTRY_DATA_WRITE));
            assertThat(back.toString(), equalTo(MOCK_ENTRY_DATA_READ));
            assertThat(back.reads, is(1));
            assertThat(back.writes, is(0));
            assertThat(cache.getEntry(), notNullValue());
            assertThat(cache.getEntry().getSize(DATA), equalTo((long) MOCK_ENTRY_DATA_READ.length()));

            try {
                IOSocket.copy(front.getInputSocket(), cache.getOutputSocket());
                if (WRITE_THROUGH != strategy) {
                    assertThat(back.writes, is(0));
                    cache.flush();
                }
                fail();
            } catch (IOException expected) {
            }
            assertThat(cache.getEntry(), notNullValue());
            assertThat(pool.entries.size(), is(1));
            assertThat(front.toString(), equalTo(MOCK_ENTRY_DATA_WRITE));
            assertThat(back.toString(), equalTo(MOCK_ENTRY_DATA_READ));
            assertThat(back.reads, is(1));
            assertThat(back.writes, is(0));
            assertThat(cache.getEntry(), notNullValue());
            assertThat(cache.getEntry().getSize(DATA), equalTo((long) MOCK_ENTRY_DATA_WRITE.length()));

            cache   .configure(back.getInputSocket())
                    .configure(back.getOutputSocket());
            assertThat(cache.getEntry(), notNullValue());
            assertThat(pool.entries.size(), is(1));
            assertThat(front.toString(), equalTo(MOCK_ENTRY_DATA_WRITE));
            assertThat(back.toString(), equalTo(MOCK_ENTRY_DATA_READ));
            assertThat(back.reads, is(1));
            assertThat(back.writes, is(0));
            assertThat(cache.getEntry(), notNullValue());
            assertThat(cache.getEntry().getSize(DATA), equalTo((long) MOCK_ENTRY_DATA_WRITE.length()));

            IOSocket.copy(front.getInputSocket(), cache.getOutputSocket());
            if (WRITE_THROUGH != strategy) {
                assertThat(back.writes, is(0));
                cache.flush();
            }
            assertThat(cache.getEntry(), notNullValue());
            assertThat(pool.entries.size(), is(1));
            assertThat(front.toString(), equalTo(MOCK_ENTRY_DATA_WRITE));
            assertThat(back.toString(), equalTo(MOCK_ENTRY_DATA_WRITE));
            assertThat(back.reads, is(1));
            assertThat(back.writes, is(1));
            assertThat(cache.getEntry(), notNullValue());
            assertThat(cache.getEntry().getSize(DATA), equalTo((long) MOCK_ENTRY_DATA_WRITE.length()));

            cache   .configure(back.getBrokenInputSocket())
                    .configure(back.getBrokenOutputSocket())
                    .clear();
            assertThat(cache.getEntry(), nullValue());
            assertThat(pool.entries.size(), is(0));
            assertThat(front.toString(), equalTo(MOCK_ENTRY_DATA_WRITE));
            assertThat(back.toString(), equalTo(MOCK_ENTRY_DATA_WRITE));
            assertThat(back.reads, is(1));
            assertThat(back.writes, is(1));
            assertThat(cache.getEntry(), nullValue());
        }
    }

    private static final class MockIOPool implements IOPool<MockIOEntry> {
        final Set<IOPool.Entry<MockIOEntry>> entries
                = new HashSet<IOPool.Entry<MockIOEntry>>();

        @Override
        public IOPool.Entry<MockIOEntry> allocate() throws IOException {
            final IOPool.Entry<MockIOEntry> entry = new MockIOEntry("") {
                @Override
                public void release() {
                    if (!entries.remove(this))
                        throw new IllegalArgumentException();
                    super.release();
                }
            };
            entries.add(entry);
            return entry;
        }

        @Override
        public void release(IOPool.Entry<MockIOEntry> entry) throws IOException {
            assert false;
            entry.release();
        }

    } // class MockIOPool

    private static class MockIOEntry implements IOPool.Entry<MockIOEntry> {
        byte[] data;
        int reads, writes;

        MockIOEntry() {
            this(null);
        }

        MockIOEntry(final String data) {
            if (null != data)
                this.data = data.getBytes();
        }

        @Override
        public String getName() {
            return MOCK_ENTRY_NAME;
        }

        @Override
        public Type getType() {
            return FILE;
        }

        @Override
        public long getSize(Size type) {
            return data.length;
        }

        @Override
        public long getTime(Access type) {
            return System.currentTimeMillis();
        }

        @Override
        public InputSocket<MockIOEntry> getInputSocket() {
            if (null == data)
                throw new IllegalStateException();
            return new MockInputSocket();
        }

        public InputSocket<MockIOEntry> getBrokenInputSocket() {
            return new BrokenInputSocket();
        }

        @Override
        public OutputSocket<MockIOEntry> getOutputSocket() {
            return new MockOutputSocket();
        }

        public OutputSocket<MockIOEntry> getBrokenOutputSocket() {
            return new BrokenOutputSocket();
        }

        @Override
        public String toString() {
            return null == data ? null : new String(data);
        }

        @Override
        public void release() {
            data = null;
        }

        private final class MockInputSocket extends InputSocket<MockIOEntry> {
            @Override
            public MockIOEntry getLocalTarget() throws IOException {
                return MockIOEntry.this;
            }

            @Override
            public ReadOnlyFile newReadOnlyFile() throws IOException {
                assertThat(getPeerTarget(), notNullValue());
                throw new UnsupportedOperationException();
            }

            @Override
            public InputStream newInputStream() throws IOException {
                assertThat(getPeerTarget(), notNullValue());
                reads++;
                return new ByteArrayInputStream(data);
            }
        } // class MockInputSocket

        private final class BrokenInputSocket extends InputSocket<MockIOEntry> {
            @Override
            public MockIOEntry getLocalTarget() throws IOException {
                return MockIOEntry.this;
            }

            @Override
            public ReadOnlyFile newReadOnlyFile() throws IOException {
                assertThat(getPeerTarget(), notNullValue());
                throw new UnsupportedOperationException();
            }

            @Override
            public InputStream newInputStream() throws IOException {
                assertThat(getPeerTarget(), notNullValue());
                return new BrokenInputStream();
            }
        } // class MockInputSocket

        private final class MockOutputSocket extends OutputSocket<MockIOEntry> {
            @Override
            public MockIOEntry getLocalTarget() throws IOException {
                return MockIOEntry.this;
            }

            @Override
            public OutputStream newOutputStream() throws IOException {
                assertThat(getPeerTarget(), notNullValue());
                writes++;
                return new MockOutputStream();
            }
        } // class MockOutputSocket

        private final class BrokenOutputSocket extends OutputSocket<MockIOEntry> {
            @Override
            public MockIOEntry getLocalTarget() throws IOException {
                return MockIOEntry.this;
            }

            @Override
            public OutputStream newOutputStream() throws IOException {
                assertThat(getPeerTarget(), notNullValue());
                return new BrokenOutputStream();
            }
        } // class MockOutputSocket

        private final class MockOutputStream extends ByteArrayOutputStream {
            @Override
            public void close() throws IOException {
                super.close();
                data = toByteArray();
            }
        } // class MockOutputStream
    } // class MockIOEntry

    private static final class BrokenInputStream extends InputStream {
        @Override
        public int read() throws IOException {
            throw new IOException();
        }
    } // class BrokenInputStream

    private static final class BrokenOutputStream extends OutputStream {
        @Override
        public void write(int b) throws IOException {
            throw new IOException();
        }
    } // class BrokenOutputStream
}
