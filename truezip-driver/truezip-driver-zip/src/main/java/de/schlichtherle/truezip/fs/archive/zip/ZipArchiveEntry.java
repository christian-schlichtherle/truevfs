/*
 * Copyright (C) 2009-2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.fs.archive.zip;

import static de.schlichtherle.truezip.entry.Entry.Access.WRITE;
import static de.schlichtherle.truezip.entry.Entry.Size.DATA;
import static de.schlichtherle.truezip.entry.Entry.Type.DIRECTORY;
import static de.schlichtherle.truezip.entry.Entry.Type.FILE;
import de.schlichtherle.truezip.fs.archive.FsArchiveEntry;
import de.schlichtherle.truezip.zip.DateTimeConverter;
import de.schlichtherle.truezip.zip.ZipEntry;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * An adapter class to make the {@link ZipEntry} class implement the
 * {@link FsArchiveEntry} interface.
 *
 * @see ZipDriver
 * @author Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
public class ZipArchiveEntry extends ZipEntry implements FsArchiveEntry {

    public ZipArchiveEntry(String name) {
        super(name);
    }

    protected ZipArchiveEntry(String name, ZipEntry template) {
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
                return FsArchiveEntry.UNKNOWN;
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
            return FsArchiveEntry.UNKNOWN;
        long time = getTime();
        return 0 <= time ? time : FsArchiveEntry.UNKNOWN;
    }

    @Override
    public boolean setTime(Access type, long time) {
        if (WRITE != type)
            return false;
        setTime(time);
        return true;
    }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        final StringBuilder s = new StringBuilder(getClass().getName())
                .append("[name=").append(getName())
                .append(",type=").append(getType());
        for (Size type : ALL_SIZE_SET)
            s.append(",size(").append(type).append(")=").append(getSize(type));
        for (Access type : ALL_ACCESS_SET)
            s.append(",time(").append(type).append(")=").append(getTime(type));
        return s.append("]").toString();
    }
}
