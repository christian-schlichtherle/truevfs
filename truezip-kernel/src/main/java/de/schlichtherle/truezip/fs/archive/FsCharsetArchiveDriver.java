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

import java.io.CharConversionException;
import de.schlichtherle.truezip.entry.Entry.Type;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import net.jcip.annotations.Immutable;

import static de.schlichtherle.truezip.entry.Entry.Type.*;
import static de.schlichtherle.truezip.fs.FsEntryName.*;
import static de.schlichtherle.truezip.io.Paths.*;

/**
 * An abstract base class of an archive driver which provides convenient
 * methods for dealing with the character set supported by a particular
 * archive type.
 * This class is intended to be used to implement archive types with a defined
 * character set, e.g. the ZIP file format with the IBM437 character set or
 * the TAR file format with the US-ASCII character set.
 * <p>
 * Sub-classes must be thread-safe and should be immutable.
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@DefaultAnnotation(NonNull.class)
public abstract class FsCharsetArchiveDriver<E extends FsArchiveEntry>
extends FsArchiveDriver<E> {

    private final Charset charset;

    /**
     * Constructs a new character set archive driver.
     *
     * @param charset the character set to use for encoding entry names and
     *        probably other meta data when writing an archive file.
     *        Depending on the archive file format, this may get used for
     *        reading an archive file, too.
     */
    protected FsCharsetArchiveDriver(Charset charset) {
        if (null == charset)
            throw new NullPointerException();
        this.charset = charset;
    }

    /**
     * Returns the character set provided to the constructor.
     * Subsequent calls must return the same object.
     *
     * @return The character set to use for encoding entry names and
     *         probably other meta data when writing an archive file.
     *         Depending on the archive file format, this may get used for
     *         reading an archive file, too.
     */
    public Charset getCharset() {
        return charset;
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
        return DIRECTORY == type
                ? name.endsWith(SEPARATOR) ? name : name + SEPARATOR_CHAR
                : cutTrailingSeparators(name, SEPARATOR_CHAR);
    }

    /**
     * Ensures that the given entry name can be encoded by this driver's
     * character set.
     * Should be called by sub classes in their implementation of the method
     * {@link FsArchiveDriver#newEntry}.
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

    private final ThreadLocalCharsetEncoder
            encoder = new ThreadLocalCharsetEncoder();

    private final class ThreadLocalCharsetEncoder
    extends ThreadLocal<CharsetEncoder> {
        @Override
        protected CharsetEncoder initialValue() {
            return getCharset().newEncoder();
        }

        boolean canEncode(CharSequence cs) {
            return get().canEncode(cs);
        }
    }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return new StringBuilder()
                .append(getClass().getName())
                .append("[charset=")
                .append(getCharset())
                .append(",federated=")
                .append(isFederated())
                .append(",priority=")
                .append(getPriority())
                .append(']')
                .toString();
    }
}
