/*
 * Copyright (C) 2009-2010 Schlichtherle IT Services
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

package de.schlichtherle.io.archive.zip;

import de.schlichtherle.io.ArchiveEntryMetaData;
import de.schlichtherle.io.archive.spi.ArchiveEntry;
import de.schlichtherle.util.zip.DateTimeConverter;
import javax.swing.Icon;

/**
 * An adapter class to make the {@link ZipEntry} class implement the
 * {@link ArchiveEntry} interface.
 *
 * @see ZipDriver
 *
 * @author Christian Schlichtherle
 * @version $Revision$
 * @since TrueZIP 6.0
 */
public class ZipEntry
        extends de.schlichtherle.util.zip.ZipEntry
        implements ArchiveEntry {

    static {
        assert de.schlichtherle.util.zip.ZipEntry.UNKNOWN == ArchiveEntry.UNKNOWN;
    }

    /** The unknown value for numeric properties. */
    public static final byte UNKNOWN = de.schlichtherle.util.zip.ZipEntry.UNKNOWN;

    private ArchiveEntryMetaData metaData;

    // TODO: Constructor should be protected!
    public ZipEntry(String entryName) {
        super(entryName);
    }

    // TODO: Constructor should be protected!
    public ZipEntry(ZipEntry blueprint) {
        super(blueprint);
    }

    /**
     * Throws an UnsupportedOperationException.
     *
     * @throws UnsupportedOperationException
     *         Cloning doesn't work with ArchiveEntryMetaData and is not
     *         required for ArchiveDriver's anyway.
     */
    public Object clone() {
        throw new UnsupportedOperationException("Cloning doesn't work with ArchiveEntryMetaData and is not required for an ArchiveDriver anyway.");
    }

    protected void setName(String name) {
        super.setName(name);
    }

    public Icon getOpenIcon() {
        return null;
    }

    public Icon getClosedIcon() {
        return null;
    }

    protected DateTimeConverter getDateTimeConverter() {
        return DateTimeConverter.ZIP;
    }

    //
    // Metadata implementation.
    //

    public ArchiveEntryMetaData getMetaData() {
        return metaData;
    }

    public void setMetaData(ArchiveEntryMetaData metaData) {
        this.metaData = metaData;
    }
}
