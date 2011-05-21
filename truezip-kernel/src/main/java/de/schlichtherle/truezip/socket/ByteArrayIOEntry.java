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

import de.schlichtherle.truezip.rof.ByteArrayReadOnlyFile;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import net.jcip.annotations.NotThreadSafe;

/**
 * An I/O entry which uses a byte array.
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
@NotThreadSafe
public class ByteArrayIOEntry implements IOEntry<ByteArrayIOEntry> {

    private final String name;
    private @CheckForNull byte[] data;
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
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("EI_EXPOSE_REP2")
    public void setData(final @CheckForNull byte[] data) {
        this.data = data;
    }

    /**
     * @return The number of times a read only file or an input stream for this
     *         I/O entry has been opened.
     */
    public int getReads() {
        return reads;
    }

    /**
     * @return The number of times an output stream for this I/O entry has been
     *         opened.
     */
    public int getWrites() {
        return writes;
    }

    @Override
    public final String getName() {
        return name;
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
    public InputSocket<ByteArrayIOEntry> getInputSocket() {
        return new ByteArrayInputSocket();
    }

    @Override
    public OutputSocket<ByteArrayIOEntry> getOutputSocket() {
        return new ByteArrayOutputSocket();
    }

    @Override
    public String toString() {
        return name;
    }

    private final class ByteArrayInputSocket extends InputSocket<ByteArrayIOEntry> {

        @Override
        public ByteArrayIOEntry getLocalTarget() throws IOException {
            return ByteArrayIOEntry.this;
        }

        @Override
        public ReadOnlyFile newReadOnlyFile() throws IOException {
            final byte[] data = ByteArrayIOEntry.this.data;
            if (null == data)
                throw new FileNotFoundException();
            reads++;
            return new ByteArrayReadOnlyFile(data);
        }

        @Override
        public InputStream newInputStream() throws IOException {
            final byte[] data = ByteArrayIOEntry.this.data;
            if (null == data)
                throw new FileNotFoundException();
            reads++;
            return new ByteArrayInputStream(data);
        }
    } // class ByteArrayInputSocket

    private class ByteArrayOutputSocket extends OutputSocket<ByteArrayIOEntry> {

        @Override
        public ByteArrayIOEntry getLocalTarget() throws IOException {
            return ByteArrayIOEntry.this;
        }

        @Override
        public OutputStream newOutputStream() throws IOException {
            writes++;
            return new DataOutputStream();
        }
    } // class ByteArrayOutputSocket

    private class DataOutputStream extends ByteArrayOutputStream {

        DataOutputStream() {
            super(initialCapacity);
        }

        @Override
        public void close() throws IOException {
            super.close();
            data = toByteArray();
        }
    } // class DataOutputStream
}
