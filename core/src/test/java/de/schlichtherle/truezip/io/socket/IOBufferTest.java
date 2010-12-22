package de.schlichtherle.truezip.io.socket;

import de.schlichtherle.truezip.io.socket.IOBuffer.Strategy;
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

import static de.schlichtherle.truezip.io.entry.Entry.Type.*;
import static de.schlichtherle.truezip.io.socket.IOBuffer.Strategy.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

public class IOBufferTest {

    private static final String MOCK_ENTRY_NAME = "mock";
    private static final String MOCK_ENTRY_DATA_READ = "read";
    private static final String MOCK_ENTRY_DATA_WRITE = "write";

    private MockIOPool pool;

    @Before
    public final void setUp() throws IOException {
        pool = new MockIOPool();
    }

    @Test
    public void testInvariants() throws IOException {
        for (final Strategy strategy : new Strategy[] {
            WRITE_THROUGH,
            WRITE_BACK,
        }) {
            final IOBuffer<MockIOEntry> buffer = strategy
                    .newIOBuffer(MockIOEntry.class, pool);
            MockIOEntry front, back;
            InputSocket<MockIOEntry> input;
            OutputSocket<MockIOEntry> output;

            back = new MockIOEntry(MOCK_ENTRY_DATA_READ);
            buffer  .configure(back.getInputSocket())
                    .configure(back.getOutputSocket());
            input = buffer.getInputSocket();
            output = buffer.getOutputSocket();
            assertThat(input.getLocalTarget(), sameInstance(back));
            assertThat(output.getLocalTarget(), sameInstance(back));

            front = new MockIOEntry();
            IOSocket.copy(input, front.getOutputSocket());
            assertThat(front.toString(), equalTo(MOCK_ENTRY_DATA_READ));

            front = new MockIOEntry(MOCK_ENTRY_DATA_WRITE);
            IOSocket.copy(front.getInputSocket(), output);
            buffer.flush();
            assertThat(back.toString(), equalTo(MOCK_ENTRY_DATA_WRITE));

            back = new MockIOEntry(MOCK_ENTRY_DATA_READ);
            buffer  .configure(back.getInputSocket())
                    .configure(back.getOutputSocket());
            input = buffer.getInputSocket();
            output = buffer.getOutputSocket();
            assertThat(input.getLocalTarget(), sameInstance(back));
            assertThat(output.getLocalTarget(), sameInstance(back));

            front = new MockIOEntry();
            IOSocket.copy(input, front.getOutputSocket());
            assertThat(front.toString(), equalTo(MOCK_ENTRY_DATA_WRITE));

            buffer.clear();
            front = new MockIOEntry();
            IOSocket.copy(input, front.getOutputSocket());
            assertThat(front.toString(), equalTo(MOCK_ENTRY_DATA_READ));

            front = new MockIOEntry(MOCK_ENTRY_DATA_WRITE);
            IOSocket.copy(front.getInputSocket(), output);
            buffer.flush();
            buffer.clear();
            assertThat(back.toString(), equalTo(MOCK_ENTRY_DATA_WRITE));

            //assertThat(pool.entries.size(), is(0));
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
                    super.release();
                    if (!entries.remove(this))
                        throw new IllegalArgumentException();
                }
            };
            entries.add(entry);
            return entry;
        }

        @Override
        public void release(IOPool.Entry<MockIOEntry> entry) throws IOException {
            if (!entries.remove(entry))
                throw new IllegalArgumentException();
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

        @Override
        public OutputSocket<MockIOEntry> getOutputSocket() {
            return new MockOutputSocket();
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

        private final class MockOutputStream extends ByteArrayOutputStream {
            @Override
            public void close() throws IOException {
                super.close();
                data = toByteArray();
            }
        } // class MockOutputStream
    } // class MockIOEntry
}
