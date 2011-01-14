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

import de.schlichtherle.truezip.entry.Entry.Access;
import de.schlichtherle.truezip.entry.Entry.Size;
import de.schlichtherle.truezip.entry.Entry.Type;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class MockIOEntry implements IOPool.Entry<MockIOEntry> {

    static final String MOCK_ENTRY_NAME = "mock";

    byte[] data;
    int reads;
    int writes;

    MockIOEntry() {
        this(null);
    }

    MockIOEntry(final String data) {
        if (null != data) {
            this.data = data.getBytes();
        }
    }

    @Override
    public String getName() {
        return MOCK_ENTRY_NAME;
    }

    @Override
    public Type getType() {
        return Type.FILE;
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
        if (null == data) {
            throw new IllegalStateException();
        }
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
}
