/*
 * Copyright (C) 2007-2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.zip;

import static de.schlichtherle.truezip.zip.Constants.*;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.HashMap;
import java.util.Map;
import net.jcip.annotations.NotThreadSafe;

/**
 * Abstract base class for an Extra Field in a Local or Central Header of a
 * ZIP archive.
 * It defines the common properties of all Extra Fields and how to
 * serialize/deserialize them to/from byte arrays.
 * 
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
@DefaultAnnotation(NonNull.class)
abstract class ExtraField {

    /** The Header ID of a ZIP64 Extended Information Extra Field. */
    static final int ZIP64_HEADER_ID = 0x0001;

    /** The Header ID a WinZip AES Extra Field. */
    static final int WINZIP_AES_ID = 0x9901;

    private static final Map<Integer, Class<? extends ExtraField>> registry
            = new HashMap<Integer, Class<? extends ExtraField>>();

    /**
     * Registers a concrete implementation of this abstract base class for
     * use with the static factory method {@link #create}.
     * 
     * @param  c the implementation class of this abstract base class.
     * @throws IllegalArgumentException if {@code c} cannot get instantiated,
     *         is not a subclass of {@code ExtraField} or its Header ID is out
     *         of range.
     *         A more descriptive exception may be associated to it as its
     *         cause.
     * @see    #create
     */
    static void register(final Class<? extends ExtraField> c) {
        final ExtraField ef;
        try {
            ef = (ExtraField) c.newInstance();
        } catch (NullPointerException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException(ex);
        }
        final int headerId = ef.getHeaderId();
        assert UShort.check(headerId);
        registry.put(headerId, c);
    }

    /**
     * A static factory method which creates a new Extra Field based on the
     * given Header ID.
     * The returned Extra Field still requires proper initialization, for
     * example by calling {@link #readFrom}.
     * 
     * @param  headerId An unsigned short integer (two bytes) which indicates
     *         the type of the returned Extra Field.
     * @return A new Extra Field
     * @throws IllegalArgumentException If {@code headerID} is out of
     *         range.
     * @see    #register
     */
    static ExtraField create(final int headerId) {
        assert UShort.check(headerId);
        final Class<? extends ExtraField> c = registry.get(headerId);
        final ExtraField ef;
        try {
            ef = null != c
                    ? (ExtraField) c.newInstance()
                    : new DefaultExtraField(headerId);
        } catch (Exception cannotHappen) {
            throw new AssertionError(cannotHappen);
        }
        assert headerId == ef.getHeaderId();
        return ef;
    }

    /**
     * Returns the Header ID (type) of this Extra Field.
     * The Header ID is an unsigned short integer (two bytes)
     * which must be constant during the life cycle of this object.
     */
    abstract int getHeaderId();

    /**
     * Returns the Data Size of this Extra Field.
     * The Data Size is an unsigned short integer (two bytes)
     * which indicates the length of the Data Block in bytes and does not
     * include its own size in this Extra Field.
     * This property may be initialized by calling {@link #readFrom}.
     * 
     * @return The size of the Data Block in bytes
     *         or {@code 0} if unknown.
     * @see #getDataBlock
     */
    abstract int getDataSize();

    /**
     * Returns a protective copy of the Data Block.
     * 
     * @see #getDataSize
     */
    final byte[] getDataBlock() {
        final int size = getDataSize();
        assert UShort.check(size);
        if (0 == size)
            return EMPTY;
        final byte[] data = new byte[size];
        writeTo(data, 0);
        return data;
    }

    /**
     * Initializes this Extra Field by deserializing a Data Block of
     * {@code size} bytes from the
     * byte array {@code data} at the zero based offset {@code off}.
     * Upon return, this Extra Field shall not access {@code data}
     * subsequently and {@link #getDataSize} must equal {@code size}.
     *
     * @param  data The byte array to read the Data Block from.
     * @param  off The zero based offset in the byte array where the first byte
     *         of the Data Block is read from.
     * @param  size The length of the Data Block in bytes.
     * @throws IndexOutOfBoundsException If the byte array
     *         {@code data} does not hold at least {@code size}
     *         bytes at the zero based offset {@code off}.
     * @throws RuntimeException If {@code size} is illegal or the
     *         deserialized Data Block contains illegal data.
     * @see    #getDataSize
     */
    abstract void readFrom(byte[] data, int off, int size);

    /**
     * Serializes a Data Block of {@link #getDataSize} bytes to the
     * byte array {@code data} at the zero based offset {@code off}.
     * Upon return, this Extra Field shall not access {@code data}
     * subsequently.
     *
     * @param  data The byte array to write the Data Block to.
     * @param  off The zero based offset in the byte array where the first byte
     *         of the Data Block is written to.
     * @throws IndexOutOfBoundsException If the byte array
     *         {@code data} does not hold at least {@link #getDataSize}
     *         bytes at the zero based offset {@code off}.
     * @see    #getDataSize
     */
    abstract void writeTo(byte[] data, int off);
}