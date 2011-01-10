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

package de.schlichtherle.truezip.io.fs.archive.driver.zip;

import de.schlichtherle.truezip.io.fs.archive.ArchiveEntry;
import de.schlichtherle.truezip.io.zip.DateTimeConverter;

import static de.schlichtherle.truezip.entry.Entry.Access.WRITE;
import static de.schlichtherle.truezip.entry.Entry.Size.DATA;
import static de.schlichtherle.truezip.entry.Entry.Type.DIRECTORY;
import static de.schlichtherle.truezip.entry.Entry.Type.FILE;

/**
 * An adapter class to make the {@link ZipEntry} class implement the
 * {@link ArchiveEntry} interface.
 *
 * @see ZipDriver
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class ZipEntry
extends de.schlichtherle.truezip.io.zip.ZipEntry
implements ArchiveEntry {

    static {
        assert de.schlichtherle.truezip.io.zip.ZipEntry.UNKNOWN
                == ArchiveEntry.UNKNOWN;
    }

    ZipEntry(String name) {
        super(name);
    }

    ZipEntry(String name, ZipEntry template) {
        super(name, template);
    }

    @Override
    public Type getType() {
        return isDirectory() ? DIRECTORY : FILE;
    }

    @Override
    protected DateTimeConverter getDateTimeConverter() {
        return DateTimeConverter.ZIP;
    }

    @Override
    public long getSize(final Size type) {
        switch (type) {
            case DATA:
                return getSize();
            case STORAGE:
                return getCompressedSize();
            default:
                return ArchiveEntry.UNKNOWN;
        }
    }

    @Override
    public boolean setSize(final Size type, final long size) {
        if (DATA != type)
            return false;
        setSize(size);
        return true;
    }

    @Override
    public long getTime(Access type) {
        if (WRITE != type)
            return ArchiveEntry.UNKNOWN;
        long time = getTime();
        return 0 <= time ? time : ArchiveEntry.UNKNOWN;
    }

    @Override
    public boolean setTime(Access type, long time) {
        if (WRITE != type)
            return false;
        setTime(time);
        return true;
    }
}
