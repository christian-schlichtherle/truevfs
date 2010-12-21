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
package de.schlichtherle.truezip.io.filesystem.file;

import de.schlichtherle.truezip.io.DecoratingInputStream;
import de.schlichtherle.truezip.io.DecoratingOutputStream;
import de.schlichtherle.truezip.io.entry.Entry;
import de.schlichtherle.truezip.io.filesystem.file.TempFilePool.TempFileEntry;
import de.schlichtherle.truezip.io.rof.DecoratingReadOnlyFile;
import de.schlichtherle.truezip.io.rof.ReadOnlyFile;
import de.schlichtherle.truezip.io.socket.DecoratingInputSocket;
import de.schlichtherle.truezip.io.socket.DecoratingOutputSocket;
import de.schlichtherle.truezip.io.socket.IOCache;
import de.schlichtherle.truezip.io.socket.IOSocket;
import de.schlichtherle.truezip.io.socket.InputCache;
import de.schlichtherle.truezip.io.socket.InputSocket;
import de.schlichtherle.truezip.io.socket.OutputCache;
import de.schlichtherle.truezip.io.socket.OutputSocket;
import de.schlichtherle.truezip.util.Pool;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Implements a caching strategy for input and output sockets.
 * Using this interface has the following effects:
 * <ul>
 * <li>Upon the first read operation, the data will be read from the local
 *     target and temporarily stored in this cache.
 *     Subsequent or concurrent read operations will be served from this cache
 *     without re-reading the data from the local target again until this cache
 *     gets {@link InputCache#clear cleared}.</li>
 * <li>At the discretion of the implementation, data written to this cache may
 *     not be written to the local target until this cache gets
 *     {@link OutputCache#flush flushed}.</li>
 * <li>After a write operation, the data will be temporarily stored in this
 *     cache for subsequent read operations until this cache gets
 *     {@link OutputCache#clear cleared}.
 * </ul>
 *
 * @param   <LT> The type of the <i>local target</i> for I/O operations.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public final class FileCache<LT extends Entry> implements IOCache<LT> {

    /** Provides different cache strategies. */
    public enum Strategy {
        /**
         * As the name implies, any attempt to create a new cache for output
         * will result in an {@link UnsupportedOperationException}.
         */
        READ_ONLY {
            @Override
            public <LT extends Entry>
            OutputCache<LT> newCache(OutputSocket <? extends LT> output) {
                throw new UnsupportedOperationException("read only cache!");
            }

            @Override
            public <LT extends Entry>
            FileCache<LT> newCache( InputSocket<? extends LT> input,
                                    OutputSocket<? extends LT> output) {
                throw new UnsupportedOperationException("read only cache!");
            }

            @Override
            <LT extends Entry>
            Pool<FileCache<LT>.Buffer, IOException> newOutputBufferPool(
                    FileCache<LT> cache) {
                throw new AssertionError();
            }
        },

        /**
         * A write-through cache flushes any written data as soon as the
         * output stream created by the provided output socket gets closed.
         */
        WRITE_THROUGH {
            @Override
            <LT extends Entry>
            Pool<FileCache<LT>.Buffer, IOException> newOutputBufferPool(
                    FileCache<LT> cache) {
                return cache.new WriteThroughOutputBufferPool();
            }
        },

        /**
         * A write-back cache flushes any written data if and only if it gets
         * explicitly flushed.
         */
        WRITE_BACK {
            @Override
            <LT extends Entry>
            Pool<FileCache<LT>.Buffer, IOException> newOutputBufferPool(
                    FileCache<LT> cache) {
                return cache.new WriteBackOutputBufferPool();
            }
        };

        /** Returns a new input cache. */
        public <LT extends Entry>
        InputCache<LT> newCache(InputSocket <? extends LT> input) {
            if (null == input)
                throw new NullPointerException();
            return new FileCache<LT>(input, null, this);
        }

        /** Returns a new output cache. */
        public <LT extends Entry>
        OutputCache<LT> newCache(OutputSocket <? extends LT> output) {
            if (null == output)
                throw new NullPointerException();
            return new FileCache<LT>(null, output, this);
        }

        /** Returns a new input / output cache. */
        public <LT extends Entry>
        FileCache<LT> newCache( InputSocket<? extends LT> input,
                                OutputSocket<? extends LT> output) {
            if (null == input || null == output)
                throw new NullPointerException();
            return new FileCache<LT>(input, output, this);
        }

        abstract <LT extends Entry>
        Pool<FileCache<LT>.Buffer, IOException> newOutputBufferPool(
                FileCache<LT> cache);
    }

    private final InputSocketProxy inputProxy;
    private final OutputSocketProxy outputProxy;
    private final Strategy strategy;
    private Pool<Buffer, IOException> inputBufferPool;
    private Pool<Buffer, IOException> outputBufferPool;
    private volatile Buffer buffer;

    FileCache(  final InputSocket <? extends LT> input,
                final OutputSocket<? extends LT> output,
                final Strategy strategy) {
        if (null == strategy)
            throw new NullPointerException();
        this.inputProxy  = null == input  ? null : new InputSocketProxy (input );
        this.outputProxy = null == output ? null : new OutputSocketProxy(output);
        this.strategy = strategy;
    }

    private Pool<Buffer, IOException> getInputBufferPool() {
        return null != inputBufferPool
                ? inputBufferPool
                : (inputBufferPool = new InputBufferPool());
    }

    private Pool<Buffer, IOException> getOutputBufferPool() {
        return null != outputBufferPool
                ? outputBufferPool
                : (outputBufferPool = strategy.newOutputBufferPool(this));
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
        if (null != buffer) { // DCL is OK in this context!
            synchronized (FileCache.this) {
                final Buffer buffer = this.buffer;
                if (null != buffer)
                    getOutputBufferPool().release(buffer);
            }
        }
    }

    @Override
    public void clear() throws IOException {
        if (null != buffer) { // DCL is OK in this context!
            synchronized (FileCache.this) {
                final Buffer buffer = this.buffer;
                if (null != buffer) {
                    // Order is important here!
                    this.buffer = null;
                    getInputBufferPool().release(buffer);
                }
            }
        }
    }

    final class InputBufferPool implements Pool<Buffer, IOException> {
        @Override
        public Buffer allocate() throws IOException {
            synchronized (FileCache.this) {
                Buffer buffer = FileCache.this.buffer;
                if (null == buffer) {
                    final InputSocket<? extends LT> input
                            = inputProxy.getBoundSocket();
                    assert null == input.getPeerTarget();
                    class ProxyOutput extends OutputSocket<Entry> {
                        TempFileEntry temp;

                        TempFileEntry getTemp() throws IOException {
                            return null != temp ? temp : (temp = TempFilePool.get().allocate());
                        }

                        @Override
                        public Entry getLocalTarget() throws IOException {
                            return Entry.NULL;
                        }

                        @Override
                        public OutputStream newOutputStream() throws IOException {
                            return FileOutputSocket.get(getTemp()).newOutputStream();
                        }
                    }
                    final ProxyOutput output = new ProxyOutput();
                    IOSocket.copy(input, output);
                    FileCache.this.buffer = buffer = new Buffer(output.getTemp());
                }
                buffer.used++;
                return buffer;
            }
        }

        @Override
        public void release(final Buffer buffer) throws IOException {
            synchronized (FileCache.this) {
                buffer.used--;
                if (buffer != FileCache.this.buffer && 0 == buffer.used)
                    buffer.file.release();
            }
        }
    } // class InputBufferPool

    abstract class OutputBufferPool implements Pool<Buffer, IOException> {
        @Override
        public Buffer allocate() throws IOException {
            return new Buffer(TempFilePool.get().allocate());
        }

        @Override
        public void release(final Buffer buffer) throws IOException {
            try {
                final OutputSocket<? extends LT> output
                        = outputProxy.getBoundSocket();
                assert null == output.getPeerTarget();
                class ProxyInput extends DecoratingInputSocket<Entry> {
                    ProxyInput() {
                        super(FileInputSocket.get(buffer.file));
                    }

                    @Override
                    public Entry getLocalTarget() throws IOException {
                        return Entry.NULL;
                    }
                }
                IOSocket.copy(new ProxyInput(), output);
            } catch (IOException ex) {
                buffer.file.release();
                throw ex;
            }
        }
    } // class OutputBufferPool

    final class WriteBackOutputBufferPool extends OutputBufferPool {
        @Override
        public Buffer allocate() throws IOException {
            synchronized (FileCache.this) {
                final Buffer buffer = super.allocate();
                buffer.dirty = true;
                return buffer;
            }
        }

        @Override
        public void release(final Buffer buffer) throws IOException {
            if (!buffer.dirty)
                return;
            synchronized (FileCache.this) {
                if (buffer != FileCache.this.buffer) {
                    FileCache.this.buffer = buffer;
                } else {
                    buffer.dirty = false;
                    super.release(buffer);
                }
            }
        }
    } // class WriteBackOutputBufferPool

    final class WriteThroughOutputBufferPool extends OutputBufferPool {
        @Override
        public void release(final Buffer buffer) throws IOException {
            if (buffer != FileCache.this.buffer) { // DCL is OK in this context!
                synchronized (FileCache.this) {
                    if (buffer != FileCache.this.buffer) {
                        FileCache.this.buffer = buffer;
                        super.release(buffer);
                    }
                }
            }
        }
    } // class WriteThroughOutputBufferPool

    final class Buffer {
        final TempFileEntry file;
        int used;
        volatile boolean dirty;

        Buffer(final TempFileEntry file) {
            this.file = file;
        }

        final class BufferReadOnlyFile extends DecoratingReadOnlyFile {
            boolean closed;

            BufferReadOnlyFile() throws IOException {
                super(FileInputSocket.get(file).newReadOnlyFile());
            }

            @Override
            public void close() throws IOException {
                if (closed)
                    return;
                closed = true;
                try {
                    delegate.close();
                } finally {
                    getInputBufferPool().release(Buffer.this);
                }
            }
        } // class BufferReadOnlyFile

        final class BufferInputStream extends DecoratingInputStream { // Do NOT extend FileIn|OutputStream: They implement finalize(), which may cause deadlocks!
            boolean closed;

            BufferInputStream() throws IOException {
                super(FileInputSocket.get(file).newInputStream());
            }

            @Override
            public void close() throws IOException {
                if (closed)
                    return;
                closed = true;
                try {
                    delegate.close();
                } finally {
                    getInputBufferPool().release(Buffer.this);
                }
            }
        } // class BufferInputStream

        final class BufferOutputStream extends DecoratingOutputStream { // Do NOT extend FileIn|OutputStream: They implement finalize(), which may cause deadlocks!
            boolean closed;

            BufferOutputStream() throws IOException {
                super(FileOutputSocket.get(file).newOutputStream());
            }

            @Override
            public void close() throws IOException {
                if (closed)
                    return;
                closed = true;
                try {
                    delegate.close();
                } finally {
                    getOutputBufferPool().release(Buffer.this);
                }
            }
        } // class BufferOutputStream
    } // class Buffer

    private final class InputSocketProxy extends DecoratingInputSocket<LT> {
        InputSocketProxy(final InputSocket <? extends LT> input) {
            super(input);
        }

        @Override
        protected InputSocket<? extends LT> getBoundSocket() throws IOException {
            return super.getBoundSocket();
        }

        @Override
        public ReadOnlyFile newReadOnlyFile() throws IOException {
            if (null != getPeerTarget()) {
                // The data for connected sockets cannot not get cached because
                // sockets may transfer different encoded data depending on
                // the identity of their peer target!
                // E.g. if the ZipDriver recognizes a ZipEntry as its peer
                // target, it transfers deflated data in order to omit
                // redundant inflating of the data from the source archive file
                // and deflating it again to the target archive file.
                // So we must flush and bypass the cache.
                flush();
                return getBoundSocket().newReadOnlyFile();
            }
            return getInputBufferPool().allocate().new BufferReadOnlyFile();
        }

        @Override
        public InputStream newInputStream() throws IOException {
            if (null != getPeerTarget()) {
                // Dito.
                flush();
                return getBoundSocket().newInputStream();
            }
            return getInputBufferPool().allocate().new BufferInputStream();
        }
    } // class InputProxy

    private final class OutputSocketProxy extends DecoratingOutputSocket<LT> {
        OutputSocketProxy(final OutputSocket<? extends LT> output) {
            super(output);
        }

        @Override
        protected OutputSocket<? extends LT> getBoundSocket() throws IOException {
            return super.getBoundSocket();
        }

        @Override
        public OutputStream newOutputStream() throws IOException {
            if (null != getPeerTarget()) {
                // Dito, but this time we must clear the cache.
                clear();
                return getBoundSocket().newOutputStream();
            }
            return getOutputBufferPool().allocate().new BufferOutputStream();
        }
    } // class OutputProxy
}
