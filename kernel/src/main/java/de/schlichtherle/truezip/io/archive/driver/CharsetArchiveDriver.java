/*
 * Copyright (C) 2006-2010 Schlichtherle IT Services
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
package de.schlichtherle.truezip.io.archive.driver;

import de.schlichtherle.truezip.io.archive.ArchiveEntry;
import de.schlichtherle.truezip.io.entry.Entry.Type;
import de.schlichtherle.truezip.io.entry.EntryFactory;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.CharConversionException;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import net.jcip.annotations.Immutable;

import static de.schlichtherle.truezip.io.filesystem.FSEntryName.*;
import static de.schlichtherle.truezip.io.Paths.*;

/**
 * Provides convenience methods for dealing with a character set.
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 */
public abstract class CharsetArchiveDriver<E extends ArchiveEntry>
extends ArchiveDriver<E> {

    /**
     * This field should be considered to be {@code final}!
     */
    private transient @NonNull Charset charset;

    /**
     * This field should be considered to be {@code final}!
     *
     * @see #assertEncodable
     */
    private transient @NonNull ThreadLocalEncoder encoder;

    /**
     * Constructs a new abstract archive driver.
     *
     * @param  charset The name of a character set to use by default for all
     *         entry names and probably other meta data when reading or writing
     *         archive files.
     */
    protected CharsetArchiveDriver(@NonNull final Charset charset) {
        if (null == charset)
            throw new NullPointerException();
        this.charset = charset;
        this.encoder = new ThreadLocalEncoder();

        assert invariants();
    }

    private boolean invariants() {
        assert null != charset;
        try {
            assertEncodable("");
        } catch (CharConversionException ex) {
            throw new AssertionError(ex);
        }
        return true;
    }

    private void writeObject(final ObjectOutputStream out)
    throws IOException {
        out.defaultWriteObject();
        out.writeUTF(charset.name());
    }

    /**
     * Postfixes the instance after its default deserialization.
     *
     * @throws InvalidObjectException If the instance invariants are not met.
     */
    private void readObject(final ObjectInputStream in)
    throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        assert charset == null;
        charset = Charset.forName(in.readUTF());
        assert encoder == null;
        encoder = new ThreadLocalEncoder();
        try {
            invariants();
        } catch (AssertionError ex) {
            throw (InvalidObjectException) new InvalidObjectException(ex.toString())
                    .initCause(ex);
        }
    }

    /**
     * Fixes the given <i>entry name</i> so that it forms a valid entry name
     * for ZIP or TAR files by ensuring that the returned entry name ends with
     * the separator character {@code '/'} if and only if {@code type} is
     * {@code DIRECTORY}.
     *
     * @param  name an entry name.
     * @param  type an entry type.
     * @return The fixed entry name.
     */
    protected static @NonNull String
    toZipOrTarEntryName(final @NonNull String name,
                        final @NonNull Type type) {
        switch (type) {
            case DIRECTORY:
                return name.endsWith(SEPARATOR) ? name : name + SEPARATOR_CHAR;
            default:
                return cutTrailingSeparators(name, SEPARATOR_CHAR);
        }
    }

    /**
     * Ensures that the given entry name can be encoded by this driver's
     * character set.
     * Should be called by sub classes in their implementation of the method
     * {@link EntryFactory#newEntry}.
     * 
     * @param  name an entry name.
     * @see    #getCharset
     * @throws CharConversionException If the path name contains characters
     *         which cannot get encoded.
     */
    protected final void assertEncodable(@NonNull String name)
    throws CharConversionException {
        if (!encoder.canEncode(name))
            throw new CharConversionException(name +
                    " (illegal characters in entry name)");
    }

    private final class ThreadLocalEncoder extends ThreadLocal<CharsetEncoder> {
        @Override
        protected CharsetEncoder initialValue() {
            return charset.newEncoder();
        }

        boolean canEncode(CharSequence cs) {
            return get().canEncode(cs);
        }
    }

    /**
     * Returns the value of the property {@code charset} which was
     * provided to the constructor.
     */
    public final @NonNull Charset getCharset() {
        return charset;
    }
}
