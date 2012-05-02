/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.tar;

import de.truezip.kernel.FsArchiveEntries;
import de.truezip.kernel.FsArchiveEntry;
import static de.truezip.kernel.cio.Entry.Access.WRITE;
import static de.truezip.kernel.cio.Entry.Size.DATA;
import static de.truezip.kernel.cio.Entry.Size.STORAGE;
import static de.truezip.kernel.cio.Entry.Type.DIRECTORY;
import static de.truezip.kernel.cio.Entry.Type.FILE;
import de.truezip.kernel.cio.IOBuffer;
import de.truezip.kernel.util.Releasable;
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
    private @CheckForNull IOBuffer<?> temp;

    public TarDriverEntry(final String name) {
        super(name, true);
        // Fix super class constructor.
        super.setUserName(System.getProperty("user.name", "TrueZIP"));
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
        super.setDevMajor(template.getDevMajor());
        super.setDevMinor(template.getDevMinor());
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

    @Nullable IOBuffer<?> getTemp() {
        return temp;
    }

    void setTemp(@CheckForNull IOBuffer<?> temp) {
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
    public Boolean isPermitted(Entity entity, Access access) {
        if (!(entity instanceof PosixEntity))
            return null;
        switch ((PosixEntity) entity) {
        case USER:
            switch (access) {
            case READ:
                return 0 != (getMode() & 00400); // TUREAD    00400     /* Read by owner         */
            case WRITE:
                return 0 != (getMode() & 00200); // TUWRITE   00200     /* Write by owner special */
            case EXECUTE:
                return 0 != (getMode() & 00100); // TUEXEC    00100     /* Execute/search by owner */
            }
            break;
        case GROUP:
            switch (access) {
            case READ:
                return 0 != (getMode() & 00040); // TGREAD    00040     /* Read by group         */
            case WRITE:
                return 0 != (getMode() & 00020); // TGWRITE   00020     /* Write by group        */
            case EXECUTE:
                return 0 != (getMode() & 00010); // TGEXEC    00010     /* Execute/search by group */
            }
            break;
        case OTHER:
            switch (access) {
            case READ:
                return 0 != (getMode() & 00004); // TOREAD    00004     /* Read by other         */
            case WRITE:
                return 0 != (getMode() & 00002); // TOWRITE   00002     /* Write by other        */
            case EXECUTE:
                return 0 != (getMode() & 00001); // TOEXEC    00001     /* Execute/search by other */
            }
        }
        return null;
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
