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
import de.schlichtherle.truezip.util.Pool;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Implements a write-back caching strategy for input and output sockets.
 * Using this class has the following effects:
 * <ul>
 * <li>Upon the first read operation, the data will be read and from the
 *     local target and stored in the cache.
 *     Subsequent or concurrent read operations will be served from the cache
 *     without re-reading the data from the local target again until the cache
 *     gets cleared.
 * <li>Any data written to the cache will get written to the local target if
 *     and only if the cache gets flushed.
 * <li>After a write operation, the data will be stored in the cache for
 *     subsequent read operations until the cache gets cleared.
 * </ul>
 * <p>
 * Note that the cache is only effective when the input and output sockets
 * are <em>not</em> connected to a peer socket!
 * 
 * @param   <LT> The type of the <i>local target</i> for I/O operations.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public final class WriteBackCache<LT extends CommonEntry> implements Cache<LT> {
    private final Lock lock = new Lock();
    private final Pool<FileEntry, IOException> pool = TempFilePool.get();
    private final InputProxy inputProxy;
    private final OutputProxy outputProxy;
    private Buffer buffer;

    public static <LT extends CommonEntry>
    InputCache<LT> newInstance(InputSocket <? extends LT> input) {
        if (null == input)
            throw new NullPointerException();
        return new WriteBackCache<LT>(input, null);
    }

    public static <LT extends CommonEntry>
    OutputCache<LT> newInstance(OutputSocket <? extends LT> output) {
        if (null == output)
            throw new NullPointerException();
        return new WriteBackCache<LT>(null, output);
    }

    public static <LT extends CommonEntry>
    Cache<LT> newInstance(InputSocket<? extends LT> input,
                          OutputSocket<? extends LT> output) {
        if (null == input || null == output)
            throw new NullPointerException();
        return new WriteBackCache<LT>(input, output);
    }

    private WriteBackCache( final InputSocket <? extends LT> input,
                            final OutputSocket<? extends LT> output) {
        this.inputProxy = input == null ? null : new InputProxy(input);
        this.outputProxy = output == null ? null : new OutputProxy(output);
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

    private final class Buffer {
        final InputChannel inputChannel = new InputChannel();
        final OutputChannel outputChannel = new OutputChannel();
        FileEntry temp;

        final class InputChannel implements Pool<Buffer, IOException> {
            int used;

            @Override
            public Buffer allocate() throws IOException {
                synchronized (lock) {
                    if (null == temp) {
                        final InputSocket<? extends LT> input
                                = inputProxy.getBoundSocket();
                        final CommonEntry peer = input.getPeerTarget();
                        class ProxyOutput extends OutputSocket<CommonEntry> {
                            FileEntry temp;

                            FileEntry getTemp() throws IOException {
                                return null != temp ? temp : (temp = pool.allocate());
                            }

                            @Override
                            public CommonEntry getLocalTarget() throws IOException {
                                return null != peer ? peer : CommonEntry.NULL;
                            }

                            @Override
                            public OutputStream newOutputStream() throws IOException {
                                return FileOutputSocket.get(getTemp()).newOutputStream();
                            }
                        }
                        final ProxyOutput output = new ProxyOutput();
                        IOSocket.copy(input, output);
                        Buffer.this.temp = output.getTemp();
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
                            final CommonEntry peer = output.getPeerTarget();
                            class ProxyInput extends FilterInputSocket<CommonEntry> {
                                ProxyInput() {
                                    super(FileInputSocket.get(temp));
                                }

                                @Override
                                public CommonEntry getLocalTarget() throws IOException {
                                    return peer;
                                }
                            }
                            IOSocket.copy(new ProxyInput(), output);
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

        final class BufferReadOnlyFile extends FilterReadOnlyFile {
            boolean closed;

            BufferReadOnlyFile() throws IOException {
                super(FileInputSocket
                        .get(inputChannel.allocate().temp)
                        .newReadOnlyFile());
            }

            @Override
            public void close() throws IOException {
                if (closed)
                    return;
                closed = true;
                inputChannel.close(rof);
            }
        } // class ReadOnlyFile

        final class BufferInputStream extends FilterInputStream { // Do NOT extend FileIn|OutputStream: They implement finalize(), which may cause deadlocks!
            boolean closed;

            BufferInputStream() throws IOException {
                super(FileInputSocket
                        .get(inputChannel.allocate().temp)
                        .newInputStream());
            }

            @Override
            public void close() throws IOException {
                if (closed)
                    return;
                closed = true;
                inputChannel.close(in);
            }
        } // class InputStream

        final class BufferOutputStream extends FilterOutputStream { // Do NOT extend FileIn|OutputStream: They implement finalize(), which may cause deadlocks!
            boolean closed;

            BufferOutputStream() throws IOException {
                super(FileOutputSocket
                        .get(outputChannel.allocate().temp)
                        .newOutputStream());
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
            return getBuffer().new BufferInputStream();
        }

        @Override
        public ReadOnlyFile newReadOnlyFile() throws IOException {
            if (null != getPeerTarget()) {
                // Dito.
                flush();
                return getBoundSocket().newReadOnlyFile();
            }
            return getBuffer().new BufferReadOnlyFile();
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
                return buffer.new BufferOutputStream();
            } catch (IOException ex) {
                buffer.inputChannel.release(buffer); // MIND inputChannel!
                throw ex;
            }
        }
    } // class OutputProxy

    private static class Lock {
    }
}
