/*
 * Copyright (C) 2010 Schlichtherle IT Services
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
package de.schlichtherle.truezip.io.socket;

import de.schlichtherle.truezip.io.FilterInputStream;
import de.schlichtherle.truezip.io.FilterOutputStream;
import de.schlichtherle.truezip.io.entry.CommonEntry;
import de.schlichtherle.truezip.io.entry.FileEntry;
import de.schlichtherle.truezip.io.entry.TempFilePool;
import de.schlichtherle.truezip.io.rof.FilterReadOnlyFile;
import de.schlichtherle.truezip.io.rof.ReadOnlyFile;
import de.schlichtherle.truezip.io.rof.SimpleReadOnlyFile;
import de.schlichtherle.truezip.util.Pool;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Implements a write-back caching strategy for input and output sockets.
 * 
 * @param   <LT> The type of the <i>local target</i> for I/O operations.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
final class DefaultCache<LT extends CommonEntry> implements Cache<LT> {
    private final Lock lock = new Lock();
    private final Pool<FileEntry, IOException> pool = TempFilePool.get();
    private final InputProxy inputProxy;
    private final OutputProxy outputProxy;
    private Buffer buffer;

    DefaultCache(   final InputSocket <? extends LT> input,
                    final OutputSocket<? extends LT> output) {
        this.inputProxy = new InputProxy(input);
        this.outputProxy = new OutputProxy(output);
    }

    @Override
    public InputSocket<LT> getInputSocket() {
        return inputProxy;
    }

    @Override
    public OutputSocket<LT> getOutputSocket() {
        return outputProxy;
    }

    @Override
    public void flush() throws IOException {
        synchronized (lock) {
            if (null != buffer)
                buffer.outputChannel.release(buffer);
        }
    }

    @Override
    public void clear() throws IOException {
        synchronized (lock) {
            final Buffer buffer = this.buffer;
            if (null != buffer) {
                // Order is important here!
                this.buffer = null;
                buffer.inputChannel.release(buffer);
            }
        }
    }

    private Buffer getBuffer() {
        synchronized (lock) {
            if (null == buffer)
                buffer = new Buffer();
            return buffer;
        }
    }

    private static class Lock {
    }

    private final class Buffer {
        final InputChannel inputChannel = new InputChannel();
        final OutputChannel outputChannel = new OutputChannel();
        FileEntry temp;

        File getFile() {
            return temp.getFile();
        }

        final class InputChannel implements Pool<Buffer, IOException> {
            int used;

            @Override
            public Buffer allocate() throws IOException {
                synchronized (lock) {
                    if (null == temp) {
                        final InputSocket<? extends LT> input
                                = inputProxy.getBoundSocket();
                        CommonEntry peer = input.getPeerTarget();
                        final FileEntry temp = pool.allocate();
                        if (null == peer)
                            peer = temp;
                        try {
                            IOSocket.copy(  input,
                                            new ProxyingOutputSocket<CommonEntry>(peer,
                                                FileOutputSocket.get(temp)));
                        } catch (IOException ex) {
                            pool.release(temp);
                            throw ex;
                        }
                        Buffer.this.temp = temp;
                    }
                    used++;
                }
                return Buffer.this;
            }

            @Override
            public void release(final Buffer resource) throws IOException {
                assert Buffer.this == resource;
                synchronized (lock) {
                    used--;
                    if (resource != buffer && 0 >= used) {
                        final FileEntry temp = resource.temp;
                        if (null != temp) {
                            resource.temp = null;
                            pool.release(temp);
                        }
                    }
                }
            }

            void close(final Closeable closeable) throws IOException {
                try {
                    closeable.close();
                } finally {
                    release(Buffer.this);
                }
            }
        } // class InputChannel

        final class OutputChannel implements Pool<Buffer, IOException> {
            volatile boolean dirty;

            @Override
            public Buffer allocate() throws IOException {
                assert null == temp;
                synchronized (lock) {
                    temp = pool.allocate();
                    dirty = true;
                }
                return Buffer.this;
            }

            @Override
            public void release(final Buffer resource) throws IOException {
                assert Buffer.this == resource;
                if (!dirty)
                    return;
                synchronized (lock) {
                    if (resource != buffer) {
                        buffer = resource;
                    } else {
                        dirty = false;
                        try {
                            final OutputSocket<? extends LT> output
                                    = outputProxy.getBoundSocket();
                            CommonEntry peer = output.getPeerTarget();
                            if (null == peer)
                                peer = temp;
                            IOSocket.copy(  new ProxyingInputSocket<CommonEntry>(peer,
                                                FileInputSocket.get(temp)),
                                            output);
                        } catch (IOException ex) {
                            pool.release(temp);
                            throw ex;
                        }
                    }
                }
            }

            void close(final Closeable closeable) throws IOException {
                try {
                    closeable.close();
                } finally {
                    release(Buffer.this);
                }
            }
        } // class OutputChannel

        final class ReadOnlyFile extends FilterReadOnlyFile {
            boolean closed;

            ReadOnlyFile() throws IOException {
                super(new SimpleReadOnlyFile(inputChannel.allocate().getFile()));
            }

            @Override
            public void close() throws IOException {
                if (closed)
                    return;
                closed = true;
                inputChannel.close(rof);
            }
        } // class ReadOnlyFile

        final class InputStream extends FilterInputStream { // Do NOT extend FileIn|OutputStream: They implement finalize(), which may cause deadlocks!
            boolean closed;

            InputStream() throws IOException {
                super(new FileInputStream(inputChannel.allocate().getFile()));
            }

            @Override
            public void close() throws IOException {
                if (closed)
                    return;
                closed = true;
                inputChannel.close(in);
            }
        } // class InputStream

        final class OutputStream extends FilterOutputStream { // Do NOT extend FileIn|OutputStream: They implement finalize(), which may cause deadlocks!
            boolean closed;

            OutputStream() throws IOException {
                super(new FileOutputStream(outputChannel.allocate().getFile()));
            }

            @Override
            public void close() throws IOException {
                if (closed)
                    return;
                closed = true;
                outputChannel.close(out);
            }
        } // class OutputStream
    } // class Buffer

    private final class InputProxy extends FilterInputSocket<LT> {
        InputProxy(final InputSocket <? extends LT> input) {
            super(input);
        }

        @Override
        public InputStream newInputStream() throws IOException {
            if (null != getPeerTarget()) {
                // The data for connected sockets cannot not be cached because
                // sockets may transfer different encoded data depending on
                // the identity of their peer target!
                // E.g. if the ZipDriver recognizes a ZipEntry as its peer
                // target, it transfers deflated data in order to omit
                // redundant inflating of the data from the source archive file
                // and deflating it again to the target archive file.
                // So we must flush and bypass the cache.
                flush();
                return getBoundSocket().newInputStream();
            }
            return getBuffer().new InputStream();
        }

        @Override
        public ReadOnlyFile newReadOnlyFile() throws IOException {
            if (null != getPeerTarget()) {
                // Dito.
                flush();
                return getBoundSocket().newReadOnlyFile();
            }
            return getBuffer().new ReadOnlyFile();
        }
    } // class InputProxy

    private final class OutputProxy extends FilterOutputSocket<LT> {
        OutputProxy(final OutputSocket<? extends LT> output) {
            super(output);
        }

        @Override
        @SuppressWarnings("ThrowableInitCause")
        public OutputStream newOutputStream() throws IOException {
            if (null != getPeerTarget()) {
                // Dito, but this time we must clear the cache.
                clear();
                return getBoundSocket().newOutputStream();
            }
            final Buffer buffer = new Buffer();
            try {
                return buffer.new OutputStream();
            } catch (IOException ex) {
                buffer.inputChannel.release(buffer); // MIND inputChannel!
                throw ex;
            }
        }
    } // class OutputProxy
}
