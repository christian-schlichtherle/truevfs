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

import de.schlichtherle.truezip.io.archive.entry.ArchiveEntry;
import de.schlichtherle.truezip.io.archive.controller.OutputArchiveMetaData;
import de.schlichtherle.truezip.io.archive.entry.ArchiveEntryContainer;
import de.schlichtherle.truezip.io.archive.entry.ArchiveEntryFactory;
import java.io.Closeable;
import java.io.FileNotFoundException;
import java.util.Iterator;

/**
 * A container which supports writing archive entries to an arbitrary output
 * destination.
 * <p>
 * Implementations do <em>not</em> need to be thread-safe:
 * Multithreading needs to be addressed by client applications.
 *
 * @see InputArchive
 * @author Christian Schlichtherle
 * @version $Id$
 */
public interface OutputArchive<AE extends ArchiveEntry>
extends ArchiveEntryContainer<AE>, Closeable {

    /**
     * {@inheritDoc}
     * <p>
     * This method may be called before the archive is closed and must also
     * reflect entries which have merely been started to be written by
     * calling {@link OutputSocket#newOutputStream}, but may not have been
     * closed yet.
     */
    @Override
    int size();

    /**
     * {@inheritDoc}
     * <p>
     * This method may be called before the archive is closed and must also
     * reflect entries which have merely been started to be written by
     * calling {@link OutputSocket#newOutputStream}, but may not have been
     * closed yet.
     */
    @Override
    Iterator<AE> iterator();

    /**
     * {@inheritDoc}
     * <p>
     * This method may be called before the archive is closed and must also
     * reflect entries which have merely been started to be written by
     * calling {@link OutputSocket#newOutputStream}, but may not have been
     * closed yet.
     */
    @Override
    AE getEntry(String name);

    /**
     * Returns a non-{@code null} reference to an output stream socket for
     * writing the given archive entry to this output archive.
     * <p>
     * The implementation must not assume that the returned output stream
     * socket will ever be used and must tolerate changes to all settable
     * properties of the {@link ArchiveEntry} interface.
     * In other words, writing an archive entry header or adding the archive
     * entry to this container merely upon the call to this method is an error.
     * <p>
     * Multiple invocations with the same parameter may return the same
     * object again.
     *
     * @param entry a non-{@code null} reference to an output stream socket
     *        for writing the given archive entry to this output archive.
     * @return A non-{@code null} reference to an output stream socket for
     *         writing the archive entry data.
     * @throws FileNotFoundException If the archive entry is not accessible.
     * @param entry A valid reference to an archive entry.
     *        The runtime class of this entry is the same as the runtime class
     *        of the entries returned by {@link ArchiveEntryFactory#newEntry}.
     */
    ArchiveOutputStreamSocket<AE> getOutputStreamSocket(AE entry)
    throws FileNotFoundException;

    /**
     * Returns the meta data for this output archive.
     * The default value is {@code null}.
     *
     * @deprecated
     */
    OutputArchiveMetaData getMetaData();

    /**
     * Sets the meta data for this output archive.
     *
     * @param metaData The meta data - may not be {@code null}.
     * @deprecated
     */
    void setMetaData(OutputArchiveMetaData metaData);
}
