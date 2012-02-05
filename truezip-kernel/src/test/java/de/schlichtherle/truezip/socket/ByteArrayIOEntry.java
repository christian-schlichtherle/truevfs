/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.socket;

import static de.schlichtherle.truezip.entry.Entry.Access.READ;
import static de.schlichtherle.truezip.entry.Entry.Access.WRITE;
import de.schlichtherle.truezip.io.SeekableByteBufferChannel;
import de.schlichtherle.truezip.rof.ByteArrayReadOnlyFile;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.util.JSE7;
import edu.umd.cs.findbugs.annotations.CleanupObligation;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import edu.umd.cs.findbugs.annotations.DischargesObligation;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.Arrays;
import java.util.EnumMap;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * An I/O entry which uses a byte array.
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
public class ByteArrayIOEntry implements IOEntry<ByteArrayIOEntry> {

    private static final SocketFactory FACTORY = JSE7.AVAILABLE
            ? SocketFactory.NIO2
            : SocketFactory.OIO;

    private final String name;
    private @Nullable byte[] data;
    private final EnumMap<Access, Long>
            times = new EnumMap<Access, Long>(Access.class);
    private int reads;
    private int writes;
    int initialCapacity;

    /**
     * Equivalent to {@link #ByteArrayIOEntry(String, int) new ByteArrayIOPool(name, 32)}.
     */
    public ByteArrayIOEntry(String name) {
        this(name, 32);
    }

    /**
     * Constructs a new byte array I/O entry with the given name and initial
     * capacity of the byte array for the next output to this I/O entry.
     *
     * @param name the name of this entry.
     * @param initialCapacity the initial capacity of the array to use for
     *        the next output to this I/O entry.
     */
    public ByteArrayIOEntry(final String name, final int initialCapacity) {
        this.name = name;
        setInitialCapacity(initialCapacity);
    }

    /**
     * Sets the initial capacity of the byte array for the next output to this
     * I/O entry.
     *
     * @param initialCapacity the initial capacity of the array to use for
     *        the next output to this I/O entry.
     */
    public final void setInitialCapacity(final int initialCapacity) {
        if (0 > initialCapacity) // Yoda conditions, I like!
            throw new IllegalArgumentException("Negative initial capacity: " + initialCapacity);
        this.initialCapacity = initialCapacity;
    }

    /**
     * Returns the byte array for input from this I/O entry.
     * This usually results from the last output and is initially {@code null}.
     * Note that the returned array is <em>not</em> copied, so beware of
     * concurrent modifications!
     *
     * @return The byte array for input from this I/O entry.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("EI_EXPOSE_REP")
    public @CheckForNull byte[] getData() {
        return data;
    }

    /**
     * Sets the byte array for input from this I/O entry.
     * Note that the given array is <em>not</em> copied, so beware of
     * concurrent modifications!
     *
     * @param data the byte array for input from this I/O entry.
     */
    @SuppressWarnings("AssignmentToCollectionOrArrayFieldFromParameter")
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("EI_EXPOSE_REP2")
    public void setData(final @CheckForNull byte[] data) {
        this.data = data;
    }

    @Override
    public final String getName() {
        return name;
    }

    @Override
    public long getSize(Size type) {
        return null != data ? data.length : UNKNOWN;
    }

    /**
     * @return The number of times an input or output socket has been used to
     *         open a connection to the backing byte array.
     */
    // http://java.net/jira/browse/TRUEZIP-83
    public int getCount(Access type) {
        return type == WRITE ? writes : reads;
    }

    /**
     * @return The last time an input or output connection to the backing byte
     *         array has been closed.
     */
    @Override
    public long getTime(Access type) {
        final Long time = times.get(type);
        return null != time ? time : UNKNOWN;
    }

    @Override
    public InputSocket<ByteArrayIOEntry> getInputSocket() {
        return FACTORY.newInputSocket(this);
    }

    @Override
    public OutputSocket<ByteArrayIOEntry> getOutputSocket() {
        return FACTORY.newOutputSocket(this);
    }

    @Override
    public String toString() {
        return name;
    }

    @Immutable
    private enum SocketFactory {
        OIO() {
            @Override
            InputSocket<ByteArrayIOEntry> newInputSocket(ByteArrayIOEntry entry) {
                return entry.new ByteArrayInputSocket();
            }

            @Override
            OutputSocket<ByteArrayIOEntry> newOutputSocket(ByteArrayIOEntry entry) {
                return entry.new ByteArrayOutputSocket();
            }
        },

        NIO2() {
            @Override
            InputSocket<ByteArrayIOEntry> newInputSocket(ByteArrayIOEntry entry) {
                return entry.new Nio2ByteArrayInputSocket();
            }

            @Override
            OutputSocket<ByteArrayIOEntry> newOutputSocket(ByteArrayIOEntry entry) {
                return entry.new Nio2ByteArrayOutputSocket();
            }
        };
        
        abstract InputSocket<ByteArrayIOEntry> newInputSocket(ByteArrayIOEntry entry);
        abstract OutputSocket<ByteArrayIOEntry> newOutputSocket(ByteArrayIOEntry entry);
    } // enum SocketFactory

    private final class Nio2ByteArrayInputSocket
    extends ByteArrayInputSocket {
        @Override
        public SeekableByteChannel newSeekableByteChannel() throws IOException {
            count();
            return new DataInputChannel();
        }
    } // class Nio2ByteArrayInputSocket

    private class ByteArrayInputSocket extends InputSocket<ByteArrayIOEntry> {
        @Override
        public final ByteArrayIOEntry getLocalTarget() throws IOException {
            return ByteArrayIOEntry.this;
        }

        final void count() throws FileNotFoundException {
            if (null == data)
                throw new FileNotFoundException();
            reads++;
        }

        @Override
        public final ReadOnlyFile newReadOnlyFile() throws IOException {
            count();
            return new DataReadOnlyFile();
        }

        @Override
        public final InputStream newInputStream() throws IOException {
            count();
            return new DataInputStream();
        }
    } // class ByteArrayInputSocket

    private class DataInputChannel extends SeekableByteBufferChannel {
        @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
        DataInputChannel() {
            super(ByteBuffer.wrap(data).asReadOnlyBuffer());
        }

        @Override
        public void close() throws IOException {
            times.put(READ, System.currentTimeMillis());
            super.close();
        }
    } // class DataInputChannel

    private class DataReadOnlyFile extends ByteArrayReadOnlyFile {
        DataReadOnlyFile() {
            super(data);
        }

        @Override
        public void close() throws IOException {
            times.put(READ, System.currentTimeMillis());
            super.close();
        }
    } // class DataReadOnlyFile

    private class DataInputStream extends ByteArrayInputStream {
        DataInputStream() {
            super(data);
        }

        @Override
        public void close() throws IOException {
            times.put(READ, System.currentTimeMillis());
            super.close();
        }
    } // class DataInputStream

    private final class Nio2ByteArrayOutputSocket
    extends ByteArrayOutputSocket {
        @Override
        public SeekableByteChannel newSeekableByteChannel() throws IOException {
            count();
            return new DataOutputChannel();
        }
    } // class Nio2ByteArrayOutputSocket

    private class ByteArrayOutputSocket extends OutputSocket<ByteArrayIOEntry> {
        @Override
        public final ByteArrayIOEntry getLocalTarget() throws IOException {
            return ByteArrayIOEntry.this;
        }

        final void count() {
            writes++;
        }

        @Override
        public final OutputStream newOutputStream() throws IOException {
            count();
            return new DataOutputStream();
        }
    } // class ByteArrayOutputSocket

    private class DataOutputChannel extends SeekableByteBufferChannel {
        @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
        DataOutputChannel() {
            super((ByteBuffer) ByteBuffer.allocate(initialCapacity).limit(0));
        }

        @Override
        public void close() throws IOException {
            times.put(WRITE, System.currentTimeMillis());
            super.close();
            final ByteBuffer buffer = getByteBuffer();
            data = Arrays.copyOf(buffer.array(), buffer.limit());
        }
    }

    private class DataOutputStream extends ByteArrayOutputStream {
        DataOutputStream() {
            super(initialCapacity);
        }

        @Override
        public void close() throws IOException {
            times.put(WRITE, System.currentTimeMillis());
            super.close();
            data = toByteArray();
        }
    } // class DataOutputStream
}
