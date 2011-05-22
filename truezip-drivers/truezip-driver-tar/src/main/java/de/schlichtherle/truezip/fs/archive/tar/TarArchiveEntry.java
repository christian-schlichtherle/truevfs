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
package de.schlichtherle.truezip.fs.archive.tar;

import static de.schlichtherle.truezip.entry.Entry.Access.WRITE;
import static de.schlichtherle.truezip.entry.Entry.Size.DATA;
import static de.schlichtherle.truezip.entry.Entry.Type.DIRECTORY;
import static de.schlichtherle.truezip.entry.Entry.Type.FILE;
import de.schlichtherle.truezip.fs.archive.FsArchiveEntry;
import de.schlichtherle.truezip.socket.IOPool.Entry;
import de.schlichtherle.truezip.util.Pool.Releasable;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;
import org.apache.tools.tar.TarEntry;

/**
 * An entry in a TAR archive which implements the {@code FsArchiveEntry}
 * interface.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
public class TarArchiveEntry
extends TarEntry
implements FsArchiveEntry, Releasable<IOException> {

    private static final Set<Type>
            FILE_SET = Collections.unmodifiableSet(EnumSet.of(FILE));
    private static final Set<Type>
            DIRECTORY_SET = Collections.unmodifiableSet(EnumSet.of(DIRECTORY));

    private Entry<?> temp;

    public TarArchiveEntry(final String name) {
        super(name, true);
        // Fix super class constructor.
        super.setModTime(Long.MIN_VALUE);
        super.setSize(UNKNOWN);
        super.setUserName(System.getProperty("user.name", ""));
    }

    public TarArchiveEntry(
            final String name,
            final TarEntry template) {
        super(name, true);
        super.setMode(template.getMode());
        super.setModTime(template.getModTime());
        super.setSize(template.getSize());
        super.setUserId(template.getUserId());
        super.setUserName(template.getUserName());
        super.setGroupId(template.getGroupId());
        super.setGroupName(template.getGroupName());
    }

    Entry<?> getTemp() {
        return temp;
    }

    void setTemp(Entry<?> temp) {
        this.temp = temp;
    }

    @Override
    public void release() throws IOException {
        if (null != temp) {
            temp.release();
            temp = null;
        }
    }

    @Override
    public Set<Type> getTypes() {
        return isDirectory() ? DIRECTORY_SET : FILE_SET;
    }

    @Override
    public long getSize(final Size type) {
        switch (type) {
            case DATA:
            case STORAGE:
                return getSize();
            default:
                return UNKNOWN;
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
            return UNKNOWN;
        long time = getModTime().getTime();
        return 0 <= time ? time : UNKNOWN;
    }

    @Override
    public boolean setTime(Access type, long time) {
        if (WRITE != type)
            return false;
        setModTime(time);
        return true;
    }

    @Override
    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    public boolean equals(Object that) {
        return super.equals(that); // make FindBugs happy!
    }

    @Override
    public int hashCode() {
        return super.hashCode(); // make FindBugs happy!
    }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        final StringBuilder s = new StringBuilder(getClass().getName())
                .append("[name=").append(getName())
                .append(",type=");//.append(BitField.copyOf(getTypes()));
        for (Iterator<Type> i = getTypes().iterator(); i.hasNext(); ) {
            s.append(i.next());
            if (i.hasNext())
                s.append('|');
        }
        for (Size type : SIZE_SET)
            s.append(",size(").append(type).append(")=").append(getSize(type));
        for (Access type : ACCESS_SET)
            s.append(",time(").append(type).append(")=").append(getTime(type));
        return s.append("]").toString();
    }
}
