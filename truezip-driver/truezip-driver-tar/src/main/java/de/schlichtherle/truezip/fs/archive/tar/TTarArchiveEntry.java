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

import static de.schlichtherle.truezip.entry.Entry.Access.*;
import static de.schlichtherle.truezip.entry.Entry.Size.*;
import static de.schlichtherle.truezip.entry.Entry.Type.*;
import de.schlichtherle.truezip.fs.archive.FsArchiveEntry;
import de.schlichtherle.truezip.socket.IOPool.Entry;
import de.schlichtherle.truezip.util.Pool.Releasable;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.IOException;
import java.util.Date;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;

/**
 * An entry in a TAR archive which implements the {@code FsArchiveEntry}
 * interface.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
public class TTarArchiveEntry
extends TarArchiveEntry
implements FsArchiveEntry, Releasable<IOException> {

    // Bit masks for initialized fields.
    private static final int SIZE = 1, MODTIME = 1 << 1;

    private byte init; // bit flags for init state
    private @CheckForNull Entry<?> temp;

    public TTarArchiveEntry(final String name) {
        super(name, true);
        // Fix super class constructor.
        super.setUserName(System.getProperty("user.name", ""));
    }

    protected TTarArchiveEntry(
            final String name,
            final TarArchiveEntry template) {
        super(name, true);
        //this.init = SIZE | MODTIME;
        super.setMode(template.getMode());
        this.setModTime0(template.getModTime().getTime());
        this.setSize0(template.getSize());
        super.setUserId(template.getUserId());
        super.setUserName(template.getUserName());
        super.setGroupId(template.getGroupId());
        super.setGroupName(template.getGroupName());
    }

    private boolean isInit(final int mask) {
        return 0 != (init & mask);
    }

    private void setInit(final int mask, final boolean init) {
        if (init)
            this.init |=  mask;
        else
            this.init &= ~mask;
    }

    @Nullable Entry<?> getTemp() {
        return temp;
    }

    void setTemp(@CheckForNull Entry<?> temp) {
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
    public Type getType() {
        return isDirectory() ? DIRECTORY : FILE;
    }

    @Override
    public final long getSize() {
        return isInit(SIZE) ? super.getSize() : UNKNOWN;
    }

    @Override
    public void setSize(long size) {
        setSize0(size);
    }

    private void setSize0(final long size) {
        final boolean known = UNKNOWN != size;
        super.setSize(known ? size : 0);
        setInit(SIZE, known);
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
    public final Date getModTime() {
        return isInit(MODTIME) ? super.getModTime() : new Date(UNKNOWN);
    }

    @Override
    public void setModTime(long time) {
        setModTime0(time);
    }

    private void setModTime0(final long time) {
        final boolean known = UNKNOWN != time;
        super.setModTime(known ? time : 0);
        setInit(MODTIME, known);
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
                .append(",type=").append(getType());
        for (Size type : ALL_SIZE_SET)
            s.append(",size(").append(type).append(")=").append(getSize(type));
        for (Access type : ALL_ACCESS_SET)
            s.append(",time(").append(type).append(")=").append(getTime(type));
        return s.append("]").toString();
    }
}
