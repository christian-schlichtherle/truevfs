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

    /**
     * Returns the value of the property {@code charset} which is used to
     * encode entry names and probably other meta data when reading or writing
     * an archive file.
     * Multiple invocations must return objects which at least compare
     * {@link Charset#equals}.
     *
     * @return A character set.
     */
    public abstract Charset getCharset();

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

    private final ThreadLocalEncoder encoder = new ThreadLocalEncoder();

    private final class ThreadLocalEncoder extends ThreadLocal<CharsetEncoder> {
        @Override
        protected CharsetEncoder initialValue() {
            return getCharset().newEncoder();
        }

        boolean canEncode(CharSequence cs) {
            return get().canEncode(cs);
        }
    }
}
