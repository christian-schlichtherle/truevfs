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
import de.schlichtherle.truezip.rof.ByteArrayReadOnlyFile;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class MockIOEntry implements IOEntry<MockIOEntry> {

    private final String name;
    private byte[] data;
    int reads;
    int writes;
    int initialCapacity = 32;

    public MockIOEntry(final String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(final byte[] data) {
        this.data = data;
    }

    public void setInitialCapacity(final int initialCapacity) {
        this.initialCapacity = initialCapacity;
    }

    @Override
    public Type getType() {
        return Type.FILE;
    }

    @Override
    public long getSize(Size type) {
        return null == data ? UNKNOWN : data.length;
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
        return name;
    }

    private final class MockInputSocket extends InputSocket<MockIOEntry> {

        @Override
        public MockIOEntry getLocalTarget() throws IOException {
            return MockIOEntry.this;
        }

        @Override
        public ReadOnlyFile newReadOnlyFile() throws IOException {
            reads++;
            return new ByteArrayReadOnlyFile(data);
        }

        @Override
        public InputStream newInputStream() throws IOException {
            reads++;
            return new ByteArrayInputStream(data);
        }
    } // class MockInputSocket

    private class MockOutputSocket extends OutputSocket<MockIOEntry> {

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

    private class MockOutputStream extends ByteArrayOutputStream {

        @Override
        public void close() throws IOException {
            super.close();
            data = toByteArray();
        }
    } // class MockOutputStream

    private class BrokenInputSocket extends InputSocket<MockIOEntry> {

        @Override
        public MockIOEntry getLocalTarget() throws IOException {
            return MockIOEntry.this;
        }

        @Override
        public ReadOnlyFile newReadOnlyFile() throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public InputStream newInputStream() throws IOException {
            return new BrokenInputStream();
        }
    } // class MockInputSocket

    private static class BrokenInputStream extends InputStream {

        @Override
        public int read() throws IOException {
            throw new IOException();
        }
    } // class BrokenInputStream

    private class BrokenOutputSocket extends OutputSocket<MockIOEntry> {

        @Override
        public MockIOEntry getLocalTarget() throws IOException {
            return MockIOEntry.this;
        }

        @Override
        public OutputStream newOutputStream() throws IOException {
            return new BrokenOutputStream();
        }
    } // class MockOutputSocket

    private static class BrokenOutputStream extends OutputStream {

        @Override
        public void write(int b) throws IOException {
            throw new IOException();
        }
    } // class BrokenOutputStream
}
