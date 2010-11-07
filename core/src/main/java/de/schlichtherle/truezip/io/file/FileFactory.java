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
package de.schlichtherle.truezip.io.file;

import java.io.FileNotFoundException;
import java.net.URI;

/**
 * A factory interface which creates {@link File}s, {@link FileInputStream}s
 * and {@link FileOutputStream}s.
 * You should not implement this interface directly - implement
 * {@link ArchiveDetector} instead.
 * <p>
 * <b>Warning:</b> This interface is <em>not</em> intended for public use!
 * It's only used to hide the existence of {@link ArchiveDetector}s from
 * some methods in the {@link File} class.
 * <p>
 * Implementations must be virtually immutable and thread safe!
 *
 * @see ArchiveDetector
 * @author Christian Schlichtherle
 * @version $Id$
 */
public interface FileFactory {

    /**
     * Constructs a new {@link File} instance from the given
     * {@code blueprint}.
     *
     * @param template The file to use as a blueprint. If this is an instance
     *        of the {@link File} class, its fields are simply copied.
     *
     * @return A newly created instance of the class {@link File}.
     */
    File newFile(java.io.File template);

    /**
     * This is used by {@link File#getParentFile()} for fast file construction
     * without rescanning the entire path for archive files, which could even
     * lead to wrong results.
     * <p>
     * <b>Warning:</b> This method is <em>not</em> intended for public use!
     * <p>
     * Calling this constructor with illegal arguments may result in
     * {@link IllegalArgumentException}, {@link AssertionError} or
     * may even silently fail!
     */
    File newFile(java.io.File delegate, File innerArchive);

    /**
     * Used for fast file construction without rescanning the pathname for
     * archive files when rewriting the pathname of an existing {@link File}
     * instance.
     * <p>
     * <b>Warning:</b> This method is <em>not</em> intended for public use!
     * <p>
     * Calling this method with illegal arguments may result in
     * {@link IllegalArgumentException}, {@link AssertionError} or
     * may even silently fail!
     */
    File newFile(File template, java.io.File delegate, File enclArchive);

    /**
     * Constructs a new {@link File} instance which uses this
     * {@link ArchiveDetector} to detect any archive files in its pathname.
     *
     * @param path The pathname of the file.
     * @return A newly created instance of the class {@link File}.
     */
    File newFile(String path);

    /**
     * Constructs a new {@link File} instance which uses this
     * {@link ArchiveDetector} to detect any archive files in its pathname.
     *
     * @param parent The parent pathname as a {@link String}.
     * @param child The child pathname as a {@link String}.
     *
     * @return A newly created instance of the class {@link File}.
     */
    File newFile(String parent, String child);

    /**
     * Constructs a new {@link File} instance which uses this
     * {@link ArchiveDetector} to detect any archive files in its pathname.
     *
     * @param parent The parent pathname as a {@code File}.
     * @param child The child pathname as a {@link String}.
     *
     * @return A newly created instance of the class {@link File}.
     */
    File newFile(java.io.File parent, String child);

    /**
     * Constructs a new {@link File} instance from the given
     * {@code uri}. This method behaves similar to
     * {@link java.io.File#File(URI) new java.io.File(uri)} with the following
     * amendment:
     * If the URI matches the pattern
     * {@code (jar:)*file:(<i>path</i>!/)*<i>entry</i>}, then the
     * constructed file object treats the URI like a (possibly ZIPped) file.
     * <p>
     * The newly created {@link File} instance uses this
     * {@link ArchiveDetector} to detect any archive files in its pathname.
     *
     * @param uri an absolute, hierarchical URI with a scheme equal to
     *        {@code file} or {@code jar}, a non-empty path component,
     *        and undefined authority, query, and fragment components.
     *
     * @return A newly created instance of the class {@link File}.
     *
     * @throws NullPointerException if {@code uri} is {@code null}.
     * @throws IllegalArgumentException if the preconditions on the
     *         parameter {@code uri} do not hold.
     */
    File newFile(URI uri);

    /**
     * Creates a new {@link FileInputStream} to read the content of the
     * given file.
     *
     * @param file The file to read.
     *
     * @return A newly created instance of the class {@link FileInputStream}.
     *
     * @throws FileNotFoundException On any I/O related issue when opening the file.
     */
    FileInputStream newFileInputStream(java.io.File file)
    throws FileNotFoundException;

    /**
     * Creates a new {@link FileOutputStream} to write the new content of the
     * given file.
     *
     * @param file The file to write.
     * @param append If {@code true} the new content should be appended
     *        to the old content rather than overwriting it.
     *
     * @return A newly created instance of the class {@link FileOutputStream}.
     *
     * @throws FileNotFoundException On any I/O related issue when opening the file.
     */
    FileOutputStream newFileOutputStream(java.io.File file, boolean append)
    throws FileNotFoundException;
}
