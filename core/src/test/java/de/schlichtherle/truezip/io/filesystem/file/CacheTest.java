package de.schlichtherle.truezip.io.filesystem.file;

import de.schlichtherle.truezip.io.socket.IOEntry;
import de.schlichtherle.truezip.io.entry.Entry;
import de.schlichtherle.truezip.io.entry.Entry.Access;
import de.schlichtherle.truezip.io.entry.Entry.Size;
import de.schlichtherle.truezip.io.entry.Entry.Type;
import de.schlichtherle.truezip.io.rof.ReadOnlyFile;
import de.schlichtherle.truezip.io.socket.IOPool;
import de.schlichtherle.truezip.io.socket.InputSocket;
import de.schlichtherle.truezip.io.socket.OutputSocket;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.junit.After;
import org.junit.Before;
import static de.schlichtherle.truezip.io.entry.Entry.Type.*;

public class CacheTest {

    private static final String MOCK_ENTRY_NAME = "mock";
    private static final String MOCK_ENTRY_DATA = "Hello World!";

    private MockIOPool pool = new MockIOPool();
    private Cache<?> cache;

    @Before
    public final void setUp() throws IOException {
        MockIOEntry entry = new MockIOEntry();
        cache = newCache(pool)
                .configure(entry.getInputSocket())
                .configure(entry.getOutputSocket());
    }

    protected <E extends IOEntry<E>> Cache<E> newCache(IOPool<E> pool) {
        return Cache.Strategy.WRITE_THROUGH.newCache(pool);
    }

    @After
    public final void tearDown() {
        cache = null;
    }

    static final class MockIOPool implements IOPool<MockIOEntry> {
        MockIOEntry entry;

        @Override
        public IOPool.Entry<MockIOEntry> allocate() throws IOException {
            return entry = new MockIOEntry();
        }

        @Override
        public void release(IOPool.Entry<MockIOEntry> resource) throws IOException {
            resource.release();
        }

    } // class MockIOPool

    private static final class MockIOEntry implements IOPool.Entry<MockIOEntry> {
        byte[] data = MOCK_ENTRY_DATA.getBytes();
        int reads, writes;

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

        @Override
        public OutputSocket<MockIOEntry> getOutputSocket() {
            if (null == data)
                throw new IllegalStateException();
            return new MockOutputSocket();
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
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public InputStream newInputStream() throws IOException {
                reads++;
                return new ByteArrayInputStream(data);
            }
        } // class MockInputSocket

        private final class MockOutputSocket extends OutputSocket<MockIOEntry> {
            @Override
            public MockIOEntry getLocalTarget() throws IOException {
                return MockIOEntry.this;
            }

            @Override
            public OutputStream newOutputStream() throws IOException {
                writes++;
                return new MockOutputStream();
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
}
