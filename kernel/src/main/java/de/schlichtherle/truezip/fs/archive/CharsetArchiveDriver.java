/*
 * Copyright (C) 2006-2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.fs.archive;

import de.schlichtherle.truezip.entry.Entry.Type;
import de.schlichtherle.truezip.entry.EntryFactory;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.CharConversionException;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import net.jcip.annotations.Immutable;

import static de.schlichtherle.truezip.fs.FsEntryName.*;
import static de.schlichtherle.truezip.io.Paths.*;

/**
 * A partial implementation of an archive driver which provides convenient
 * methods for dealing with the character set supported by the respective
 * archive type.
 * <p>
 * This class is only useful for archive types with a defined character set,
 * e.g. the ZIP file format with its IBM437 character set or the TAR file
 * format with its US-ASCII character set.
 * <p>
 * Implementations must be immutable.
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@DefaultAnnotation(NonNull.class)
public abstract class CharsetArchiveDriver<E extends ArchiveEntry>
extends ArchiveDriver<E> {

    private final Charset charset;
    private final ThreadLocalEncoder encoder;

    /**
     * Constructs a new abstract archive driver.
     *
     * @param  charset The name of a character set to use by default for all
     *         entry names and probably other meta data when reading or writing
     *         archive files.
     */
    protected CharsetArchiveDriver(final Charset charset) {
        if (null == charset)
            throw new NullPointerException();
        this.charset = charset;
        this.encoder = new ThreadLocalEncoder();
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
    protected static String toZipOrTarEntryName(String name, Type type) {
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
    protected final void assertEncodable(String name)
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
    public final Charset getCharset() {
        return charset;
    }
}
