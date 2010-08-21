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

import de.schlichtherle.truezip.io.OutputArchiveMetaData;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;

/**
 * Defines the interface used to write entries to an archive file.
 * <p>
 * Implementations do <em>not</em> need to be thread safe:
 * Multithreading must be addressed by the client classes.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public interface OutputArchive {

    /**
     * Returns the number of {@link ArchiveEntry} instances in this archive.
     * <p>
     * This method may be called before the archive is closed and must also
     * reflect entries which have not yet been closed.
     */
    int getNumArchiveEntries();

    /**
     * Returns an enumeration of the {@link ArchiveEntry} instances in this
     * archive (i.e. written so far).
     * <p>
     * This method may be called before the archive is closed and must also
     * reflect entries which have not yet been closed.
     */
    Enumeration getArchiveEntries();

    /**
     * Returns the {@link ArchiveEntry} for the given entry name or
     * {@code null} if no entry with this name has been written
     * or started to be written.
     * <p>
     * This method may be called before the archive is closed and must also
     * reflect entries which have not yet been closed.
     * 
     * @param entryName A valid archive entry name - never {@code null}.
     * @see <a href="ArchiveEntry.html#entryName">Requirements for Archive Entry Names</a>
     */
    ArchiveEntry getArchiveEntry(String entryName);

    /**
     * Returns a new {@code OutputStream} for writing the contents of the
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
     *        of the entries returned by
     *        {@link ArchiveDriver#createArchiveEntry}.
     * @param srcEntry If not {@code null}, this identifies the entry
     *        from which TrueZIP is actually copying data from and should be
     *        used to implement the Direct Data Copying (DDC) feature.
     *        Note that there is no guarantee on the runtime type of this
     *        object; it may have been created by other drivers.
     *        Furthermore, this <em>not</em> exclusively used for archive
     *        copies, so you should <em>not</em> simply copy all properties
     *        of the source entry to the entry (see
     *        {@link ArchiveDriver#createArchiveEntry(Archive, String, ArchiveEntry)}
     *        for comparison).
     *        <p>
     *        For example, the ZIP driver family uses this to copy the
     *        already deflated data if the source entry is another
     *        ZIP file entry.
     *        As another example, the {@link de.schlichtherle.truezip.io.archive.driver.tar.TarDriver}
     *        uses this to determine the size of the input file, thereby
     *        removing the need to create (yet another) temporary file.
     * @return A (preferrably unbuffered) {@link OutputStream} to write the
     *         archive entry data to.
     *         {@code null} is not allowed!
     * @throws OutputArchiveBusyException If the archive is currently busy
     *         on output for another entry.
     *         This exception is guaranteed to be recoverable, meaning it
     *         should be possible to write the same entry again as soon as
     *         the archive is not busy on output anymore.
     * @throws FileNotFoundException If the archive entry is not accessible
     *         for some reason.
     * @throws IOException On any other exceptional condition.
     */
    OutputStream getOutputStream(ArchiveEntry entry, ArchiveEntry srcEntry)
    throws OutputArchiveBusyException, FileNotFoundException, IOException;
    
    /**
     * Closes this output archive and releases any system resources
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
    OutputArchiveMetaData getMetaData();

    /**
     * Sets the meta data for this input archive.
     *
     * @param metaData The meta data - may not be {@code null}.
     */
    void setMetaData(OutputArchiveMetaData metaData);
}
