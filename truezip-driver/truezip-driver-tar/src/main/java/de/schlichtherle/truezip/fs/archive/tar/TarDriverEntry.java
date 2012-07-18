/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.archive.tar;

import static de.schlichtherle.truezip.entry.Entry.Access.WRITE;
import static de.schlichtherle.truezip.entry.Entry.Size.DATA;
import static de.schlichtherle.truezip.entry.Entry.Size.STORAGE;
import static de.schlichtherle.truezip.entry.Entry.Type.DIRECTORY;
import static de.schlichtherle.truezip.entry.Entry.Type.FILE;
import de.schlichtherle.truezip.fs.FsArchiveEntries;
import de.schlichtherle.truezip.fs.FsArchiveEntry;
import de.schlichtherle.truezip.socket.IOPool.Entry;
import de.schlichtherle.truezip.util.Pool.Releasable;
import java.io.IOException;
import java.util.Date;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;

/**
 * An entry in a TAR archive which implements the {@code FsArchiveEntry}
 * interface.
 *
 * @author Christian Schlichtherle
 */
public class TarDriverEntry
extends TarArchiveEntry
implements FsArchiveEntry, Releasable<IOException> {

    // Bit masks for initialized fields.
    private static final int SIZE = 1, MODTIME = 1 << 1;

    private byte init; // bit flags for init state
    private @CheckForNull Entry<?> temp;

    public TarDriverEntry(final String name) {
        super(name, true);
        // Fix super class constructor.
        super.setUserName(System.getProperty("user.name", ""));
    }

    protected TarDriverEntry(
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
        super.setLinkName(template.getLinkName());
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
    public long getSize() {
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
    public Date getModTime() {
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
    public void setModTime(Date time) {
        setModTime(time.getTime());
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
        return FsArchiveEntries.toString(this);
    }
}