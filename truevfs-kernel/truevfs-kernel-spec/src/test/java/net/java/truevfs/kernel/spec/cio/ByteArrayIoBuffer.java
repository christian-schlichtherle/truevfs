/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec.cio;

import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import net.java.truecommons.io.ByteBufferChannel;
import net.java.truecommons.io.DisconnectingInputStream;
import net.java.truecommons.io.DisconnectingOutputStream;
import net.java.truecommons.io.DisconnectingSeekableChannel;
import net.java.truevfs.kernel.spec.cio.Entry.Access;
import static net.java.truevfs.kernel.spec.cio.Entry.Access.READ;
import static net.java.truevfs.kernel.spec.cio.Entry.Access.WRITE;
import net.java.truevfs.kernel.spec.cio.Entry.Entity;
import net.java.truevfs.kernel.spec.cio.Entry.Size;
import static net.java.truevfs.kernel.spec.cio.Entry.UNKNOWN;

/**
 * An I/O buffer which is backed by a byte array.
 * <p>
 * The reference to the {@linkplain #getData() backing array} may be set to
 * {@code null}, in which case any attempt to start input from this I/O buffer
 * will result in a {@link FileNotFoundException}.
 * The reference gets updated upon each call to {@code close()} on any
 * {@link OutputStream} or {@link SeekableByteChannel}.
 * It can also get explicitly set by calling the constructor
 * {@link #ByteArrayIoBuffer(String, byte[])} or the method
 * {@link #setData(byte[])}.
 * 
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public class ByteArrayIoBuffer implements IoBuffer {

    private final String name;
    private int initialCapacity;
    private @Nullable byte[] data;
    private final EnumMap<Access, Long> times = new EnumMap<>(Access.class);
    private int reads;
    private int writes;

    /**
     * Constructs a new byte array I/O buffer.
     * The reference to the {@linkplain #getData() backing array} is set to
     * {@code null} by this constructor.
     *
     * @param name the name of this I/O buffer.
     * @param initialCapacity the initial capacity of the next backing array
     *        to allocate when starting output to this I/O buffer.
     */
    public ByteArrayIoBuffer(String name, int initialCapacity) {
        this(name, null, initialCapacity);
    }

    /**
     * Constructs a new byte array I/O buffer.
     * The {@linkplain #getInitialCapacity() initial capacity} will be set to
     * length of the given backing array.
     * Note that the given backing array does <em>not</em> get copied, so
     * beware of concurrent modifications!
     *
     * @param name the name of this I/O buffer.
     * @param data the backing array.
     */
    public ByteArrayIoBuffer(String name, byte[] data) {
        this(name, data, data.length);
    }

    private ByteArrayIoBuffer(  final String name,
                                final @CheckForNull byte[] data,
                                final int initialCapacity) {
        this.name = Objects.requireNonNull(name);
        setData(data);
        setInitialCapacity(initialCapacity);
    }

    /**
     * Returns the initial capacity of the next backing array to allocate when
     * starting output to this I/O buffer.
     * 
     * @return The initial capacity of the next backing array to allocate when
     *         starting output to this I/O buffer.
     */
    public final int getInitialCapacity() {
        return this.initialCapacity;
    }

    /**
     * Sets the initial capacity of the next backing array to allocate when
     * starting output to this I/O buffer.
     *
     * @param initialCapacity the initial capacity of the next backing array
     *        to allocate when starting output to this I/O buffer.
     */
    public final void setInitialCapacity(final int initialCapacity) {
        if (0 > initialCapacity)
            throw new IllegalArgumentException("Negative initial capacity: " + initialCapacity);
        this.initialCapacity = initialCapacity;
    }

    /**
     * Returns the nullable backing array.
     * Note that the returned backing array does <em>not</em> get copied, so
     * beware of concurrent modifications!
     *
     * @return The nullable backing array.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("EI_EXPOSE_REP")
    public final @Nullable byte[] getData() {
        return data;
    }

    /**
     * Sets the nullable backing array.
     * Note that the given backing array does <em>not</em> get copied, so
     * beware of concurrent modifications!
     *
     * @param data the nullable backing array.
     */
    @SuppressWarnings("AssignmentToCollectionOrArrayFieldFromParameter")
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("EI_EXPOSE_REP2")
    public final void setData(final @CheckForNull byte[] data) {
        this.data = data;
    }

    @Override
    public final String getName() {
        return name;
    }

    @Override
    public final long getSize(Size type) {
        return null != data ? data.length : UNKNOWN;
    }

    /**
     * @param  type the access type.
     * @return The number of times an input or output socket has been used to
     *         open a connection to the backing byte array.
     */
    // http://java.net/jira/browse/TRUEZIP-83
    public final int getCount(Access type) {
        return type == WRITE ? writes : reads;
    }

    /**
     * @return The last time an input or output connection to the backing byte
     *         array has been closed.
     */
    @Override
    public final long getTime(Access type) {
        final Long time = times.get(type);
        return null != time ? time : UNKNOWN;
    }

    @Override
    public Boolean isPermitted(Access type, Entity entity) {
        return true;
    }

    @Override
    public final InputSocket<ByteArrayIoBuffer> input() {
        return new Input();
    }

    @Override
    public final OutputSocket<ByteArrayIoBuffer> output() {
        return new Output();
    }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return String.format("%s[name=%s]",
                getClass().getName(),
                getName());
    }

    @Override
    public void release() throws IOException {
    }

    private final class Input extends AbstractInputSocket<ByteArrayIoBuffer> {
        @Override
        public ByteArrayIoBuffer target() throws IOException {
            return ByteArrayIoBuffer.this;
        }

        void count() throws FileNotFoundException {
            if (null == data)
                throw new FileNotFoundException();
            reads++;
        }

        @Override
        public InputStream stream(OutputSocket<? extends Entry> _)
        throws IOException {
            count();
            return new DataInputStream();
        }

        @Override
        public SeekableByteChannel channel(OutputSocket<? extends Entry> _)
        throws IOException {
            count();
            return new DataInputChannel();
        }
    } // Input

    private final class Output extends AbstractOutputSocket<ByteArrayIoBuffer> {
        @Override
        public ByteArrayIoBuffer target() throws IOException {
            return ByteArrayIoBuffer.this;
        }

        void count() {
            writes++;
        }

        @Override
        public SeekableByteChannel channel(InputSocket<? extends Entry> _)
        throws IOException {
            count();
            return new DataOutputChannel();
        }

        @Override
        public OutputStream stream(InputSocket<? extends Entry> _)
        throws IOException {
            count();
            return new DataOutputStream();
        }
    } // Output

    private final class DataInputChannel extends DisconnectingSeekableChannel {
        boolean closed;

        @CreatesObligation
        DataInputChannel() {
            super(new ByteBufferChannel(ByteBuffer.wrap(data).asReadOnlyBuffer()));
        }

        @Override
        public boolean isOpen() { return !closed; }

        @Override
        public void close() throws IOException {
            if (closed) return;
            channel.close();
            times.put(READ, System.currentTimeMillis());
            closed = true;
        }
    } // DataInputChannel

    private final class DataOutputChannel extends DisconnectingSeekableChannel {
        boolean closed;

        @CreatesObligation
        DataOutputChannel() {
            super(new ByteBufferChannel((ByteBuffer) ByteBuffer.allocate(initialCapacity).limit(0)));
        }

        @Override
        public boolean isOpen() { return !closed; }

        @Override
        public void close() throws IOException {
            if (closed) return;
            channel.close();
            times.put(WRITE, System.currentTimeMillis());
            final ByteBuffer buffer = ((ByteBufferChannel) channel).bufferDuplicate();
            data = Arrays.copyOf(buffer.array(), buffer.limit());
            closed = true;
        }
    } // DataOutputChannel

    private final class DataInputStream extends DisconnectingInputStream {
        boolean closed;

        DataInputStream() { super(new ByteArrayInputStream(data)); }

        @Override
        public boolean isOpen() { return !closed; }

        @Override
        public void close() throws IOException {
            if (closed) return;
            in.close();
            times.put(READ, System.currentTimeMillis());
            closed = true;
        }
    } // DataInputStream

    private final class DataOutputStream extends DisconnectingOutputStream {
        boolean closed;

        DataOutputStream() { super(new ByteArrayOutputStream(initialCapacity)); }

        @Override
        public boolean isOpen() { return !closed; }

        @Override
        public void close() throws IOException {
            if (closed) return;
            out.close();
            times.put(WRITE, System.currentTimeMillis());
            data = ((ByteArrayOutputStream) out).toByteArray();
            closed = true;
        }
    } // DataOutputStream
}
