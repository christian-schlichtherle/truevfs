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

import de.schlichtherle.truezip.io.filesystem.FileSystemController;
import de.schlichtherle.truezip.io.filesystem.MountPoint;
import de.schlichtherle.truezip.io.filesystem.concurrent.ContentCachingFileSystemController;
import de.schlichtherle.truezip.io.filesystem.concurrent.ConcurrentFileSystemController;
import de.schlichtherle.truezip.io.archive.controller.DefaultArchiveController;
import de.schlichtherle.truezip.io.filesystem.concurrent.ConcurrentFileSystemModel;
import de.schlichtherle.truezip.io.archive.entry.ArchiveEntry;
import de.schlichtherle.truezip.io.entry.Entry.Type;
import de.schlichtherle.truezip.io.entry.EntryFactory;
import de.schlichtherle.truezip.io.filesystem.file.TempFilePool;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.CharConversionException;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import javax.swing.Icon;

import static de.schlichtherle.truezip.io.filesystem.FileSystemEntryName.*;
import static de.schlichtherle.truezip.io.Paths.*;

/**
 * An abstract archive driver implementation to ease the task of developing
 * an archive driver.
 * It provides default implementations for character sets and icon handling.
 * <p>
 * This class is serializable in order to meet the requirements of some client
 * classes.
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 */
public abstract class AbstractArchiveDriver<E extends ArchiveEntry>
implements ArchiveDriver<E>, Serializable {

    private static final long serialVersionUID = 6546816446546846516L;

    /**
     * This field should be considered to be {@code final}!
     */
    @NonNull
    private transient Charset charset;

    /**
     * This field should be considered to be {@code final}!
     *
     * @see #assertEncodable
     */
    @NonNull
    private transient ThreadLocalEncoder encoder;

    /**
     * Constructs a new abstract archive driver.
     *
     * @param  charset The name of a character set to use by default for all
     *         entry names and probably other meta data when reading or writing
     *         archive files.
     */
    protected AbstractArchiveDriver(@NonNull final Charset charset) {
        if (null == charset)
            throw new NullPointerException();
        this.charset = charset;
        this.encoder = new ThreadLocalEncoder();

        assert invariants();
    }

    /**
     * Checks the invariants of this class and throws an AssertionError if
     * any is violated even if assertion checking is disabled.
     * <p>
     * The constructors call this method like this:
     * <pre>{@code assert invariants(); }</pre>
     * This calls the method if and only if assertions are enabled in order
     * to assert that the instance invariants are properly obeyed.
     * If assertions are disabled, the call to this method is thrown away by
     * the HotSpot compiler, so there is no performance penalty.
     * <p>
     * When deserializing however, this method is called regardless of the
     * assertion status. On error, the {@link AssertionError} is wrapped
     * in an {@link InvalidObjectException} and thrown instead.
     *
     * @throws AssertionError If any invariant is violated even if assertions
     *         are disabled.
     * @return {@code true}
     */
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
        out.writeObject(charset.name());
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
        charset = Charset.forName((String) in.readObject());
        assert encoder == null;
        encoder = new ThreadLocalEncoder();
        try {
            invariants();
        } catch (AssertionError ex) {
            throw (InvalidObjectException) new InvalidObjectException(ex.toString()).initCause(ex);
        }
    }

    /**
     * Maps the given <i>path name</i> to an <i>entry name</i> for ZIP or TAR
     * files by ensuring that the returned entry name ends with the separator
     * character {@code '/'} if and only if {@code type} is {@code DIRECTORY}.
     * <p>
     * First, {@link #assertEncodable(String) assertEncodable(path)} is called.
     *
     * @see    EntryFactory#newEntry Common Requirements For Path Names
     * @param  path a non-{@code null} <i>path name</i>.
     * @param  type a non-{@code null} entry type.
     * @return A non-{@code null} <i>entry name</i>.
     */
    protected final String toZipOrTarEntryName(
            final String path,
            final Type type)
    throws CharConversionException {
        assertEncodable(path);
        switch (type) {
            case DIRECTORY:
                return path.endsWith(SEPARATOR) ? path : path + SEPARATOR_CHAR;
            default:
                return cutTrailingSeparators(path, SEPARATOR_CHAR);
        }
    }

    /**
     * Ensures that the given path name can be encoded by this driver's
     * character set.
     * Should be called by sub classes in their implementation of the method
     * {@link EntryFactory#newEntry}.
     * 
     * @see    EntryFactory#newEntry Common Requirements For Path Names
     * @param  path a non-{@code null} path name.
     * @see    #getCharset
     * @throws CharConversionException If the path name contains characters
     *         which cannot get encoded.
     */
    protected final void assertEncodable(String path)
    throws CharConversionException {
        if (!encoder.canEncode(path))
            throw new CharConversionException(path +
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

    @Override
    @NonNull public FileSystemController<?>
    newController(  @NonNull MountPoint mountPoint,
                    @NonNull FileSystemController<?> parent) {
        return  new ConcurrentFileSystemController<ConcurrentFileSystemModel, FileSystemController<? extends ConcurrentFileSystemModel>>(
                    //new IOSocketCachingFileSystemController<ConcurrentFileSystemModel, FileSystemController<? extends ConcurrentFileSystemModel>>(
                        new ContentCachingFileSystemController<ConcurrentFileSystemModel, FileSystemController<? extends ConcurrentFileSystemModel>>(
                            new DefaultArchiveController<E>(
                                new ConcurrentFileSystemModel(mountPoint, parent.getModel()),
                                this, parent, false),
                            TempFilePool.get()));
    }

    /**
     * Returns the value of the property {@code charset} which was
     * provided to the constructor.
     */
    @NonNull
    public final Charset getCharset() {
        return charset;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@code AbstractArchiveDriver} simply
     * returns {@code null}.
     *
     * @param model ignored.
     */
    @Override
    public Icon getOpenIcon(ConcurrentFileSystemModel model) {
        return null;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@code AbstractArchiveDriver} simply
     * returns {@code null}.
     */
    @Override
    public Icon getClosedIcon(ConcurrentFileSystemModel model) {
        return null;
    }
}
