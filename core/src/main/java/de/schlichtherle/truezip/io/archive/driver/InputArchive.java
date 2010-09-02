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
import de.schlichtherle.truezip.io.archive.controller.InputArchiveMetaData;
import de.schlichtherle.truezip.io.archive.entry.ArchiveEntryContainer;
import java.io.Closeable;
import java.io.FileNotFoundException;

/**
 * A container which supports reading archive entries from an arbitrary input
 * source.
 * <p>
 * Implementations do <em>not</em> need to be thread-safe:
 * Multithreading needs to be addressed by client applications.
 *
 * @see OutputArchive
 * @author Christian Schlichtherle
 * @version $Id$
 */
public interface InputArchive<AE extends ArchiveEntry>
extends ArchiveEntryContainer<AE>, Closeable {

    /**
     * Returns a non-{@code null} reference to an input stream socket for
     * reading the given archive entry from this input archive.
     * The archive entry is guaranteed to be one of the entries in this
     * container.
     * <p>
     * The implementation must not assume that the returned input stream
     * socket will ever be used and must tolerate changes to all settable
     * properties of the {@link ArchiveEntry} interface since it has loaded
     * the entries from its input source.
     * <p>
     * Multiple invocations with the same parameter may return the same
     * object again.
     *
     * @param entry a non-{@code null} reference to an input stream socket
     *        for reading the given archive entry from this input archive.
     * @return A non-{@code null} reference to an input stream socket for
     *         reading the archive entry data.
     * @throws FileNotFoundException If the archive entry does not exist or is
     *         not accessible.
     */
    ArchiveInputStreamSocket<AE> getInputStreamSocket(AE entry)
    throws FileNotFoundException;

    /**
     * Returns the meta data for this input archive.
     * The default value is {@code null}.
     *
     * @deprecated
     */
    InputArchiveMetaData getMetaData();

    /**
     * Sets the meta data for this input archive.
     *
     * @param metaData the meta data - may not be {@code null}.
     *
     * @deprecated
     */
    void setMetaData(InputArchiveMetaData metaData);
}
