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

import de.schlichtherle.truezip.io.archive.controller.InputArchiveMetaData;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;

/**
 * Defines the interface used to read entries from an archive file.
 * <p>
 * Implementations do <em>not</em> need to be thread safe:
 * Multithreading is addressed in the package {@code de.schlichtherle.truezip.io}.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public interface InputArchive {

    /**
     * Returns the number of {@link ArchiveEntry} instances in this archive.
     */
    int getNumArchiveEntries();

    /**
     * Returns an enumeration of the {@link ArchiveEntry} instances in this
     * archive.
     */
    Enumeration<ArchiveEntry> getArchiveEntries();

    /**
     * Returns the {@link ArchiveEntry} for the given entry name or
     * {@code null} if no entry with this name exists.
     * 
     * @param entryName A valid archive entry name - never {@code null}.
     * @see <a href="ArchiveEntry.html#entryName">Requirements for Archive Entry Names</a>
     */
    ArchiveEntry getArchiveEntry(String entryName);

    /**
     * Returns a new {@code InputStream} for reading the contents of the
     * given archive entry.
     * <p>
     * The returned stream should preferrably be unbuffered, as buffering is
     * usually done in higher layers (all copy routines in TrueZIP do this
     * and most client applications do it, too).
     * Buffering twice does not increase, but decrease performance.
     * <p>
     * Note that the stream is guaranteed to be closed before the
     * {@link #close()} method of this archive is called!
     * 
     * @param entry A valid reference to an archive entry.
     *        The runtime class of this entry is the same as the runtime class
     *        of the entries returned by {@link #getArchiveEntries}.
     * @param dstEntry If not {@code null}, this identifies the entry
     *        to which TrueZIP is actually copying data to and should be
     *        used to implement the Direct Data Copying (DDC) feature.
     *        Note that there is no guarantee on the runtime type of this
     *        object; it may have been created by other drivers.
     *        <p>
     *        For example, the ZIP driver family uses this to determine
     *        if data should be provided in its deflated form if the
     *        destination entry is another ZIP file entry.
     * @return A (preferrably unbuffered) {@link InputStream} to read the
     *         archive entry data from.
     *         {@code null} is not allowed!
     * @throws InputArchiveBusyException If the archive is currently busy
     *         on input for another entry.
     *         This exception is guaranteed to be recoverable, meaning it
     *         should be possible to read the same entry again as soon as
     *         the archive is not busy on input anymore.
     * @throws FileNotFoundException If the archive entry does not exist or
     *         is not accessible for some reason.
     * @throws IOException On any other exceptional condition.
     */
    InputStream getInputStream(ArchiveEntry entry, ArchiveEntry dstEntry)
    throws InputArchiveBusyException, FileNotFoundException, IOException;

    /**
     * Closes this input archive and releases any system resources
     * associated with it.
     * 
     * @throws IOException On any I/O related issue.
     */
    void close()
    throws IOException;

    /**
     * Returns the meta data for this input archive.
     * The default value is {@code null}.
     */
    InputArchiveMetaData getMetaData();

    /**
     * Sets the meta data for this input archive.
     *
     * @param metaData The meta data - may not be {@code null}.
     */
    void setMetaData(InputArchiveMetaData metaData);
}
