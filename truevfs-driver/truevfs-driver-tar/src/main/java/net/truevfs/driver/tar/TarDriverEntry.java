/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.tar;

import net.truevfs.kernel.FsArchiveEntries;
import net.truevfs.kernel.FsArchiveEntry;
import static net.truevfs.kernel.cio.Entry.Access.WRITE;
import static net.truevfs.kernel.cio.Entry.Size.DATA;
import static net.truevfs.kernel.cio.Entry.Size.STORAGE;
import static net.truevfs.kernel.cio.Entry.Type.DIRECTORY;
import static net.truevfs.kernel.cio.Entry.Type.FILE;
import net.truevfs.kernel.cio.IOBuffer;
import net.truevfs.kernel.util.Releasable;
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

    private static final int TUREAD  = 0400; // Read by owner
    private static final int TUWRITE = 0200; // Write by owner special
    private static final int TUEXEC  = 0100; // Execute/search by owner
    private static final int TGREAD  = 0040; // Read by group
    private static final int TGWRITE = 0020; // Write by group
    private static final int TGEXEC  = 0010; // Execute/search by group
    private static final int TOREAD  = 0004; // Read by other
    private static final int TOWRITE = 0002; // Write by other
    private static final int TOEXEC  = 0001; // Execute/search by other

    private byte init; // bit flags for init state
    private @CheckForNull IOBuffer<?> temp;

    public TarDriverEntry(final String name) {
        super(name, true);
        // Fix super class constructor.
        super.setUserName(System.getProperty("user.name", "TrueVFS"));
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
    public Boolean isPermitted(final Access type, final Entity entity) {
        if (!(entity instanceof PosixEntity))
            return null;
        switch ((PosixEntity) entity) {
        case USER:
            switch (type) {
            case READ:
                return 0 != (super.getMode() & TUREAD);
            case WRITE:
                return 0 != (super.getMode() & TUWRITE);
            case EXECUTE:
                return 0 != (super.getMode() & TUEXEC);
            }
            break;
        case GROUP:
            switch (type) {
            case READ:
                return 0 != (super.getMode() & TGREAD);
            case WRITE:
                return 0 != (super.getMode() & TGWRITE);
            case EXECUTE:
                return 0 != (super.getMode() & TGEXEC);
            }
            break;
        case OTHER:
            switch (type) {
            case READ:
                return 0 != (super.getMode() & TOREAD);
            case WRITE:
                return 0 != (super.getMode() & TOWRITE);
            case EXECUTE:
                return 0 != (super.getMode() & TOEXEC);
            }
        }
        return null;
    }

    @Override
    public boolean setPermitted(
            final Access type,
            final Entity entity,
            final Boolean value) {
        if (null == value || !(entity instanceof PosixEntity))
            return false;
        switch ((PosixEntity) entity) {
        case USER:
            switch (type) {
            case READ:
                super.setMode(value ? super.getMode() | TUREAD : super.getMode() & ~TUREAD);
                return true;
            case WRITE:
                super.setMode(value ? super.getMode() | TUWRITE : super.getMode() & ~TUWRITE);
                return true;
            case EXECUTE:
                super.setMode(value ? super.getMode() | TUEXEC : super.getMode() & ~TUEXEC);
                return true;
            }
            break;
        case GROUP:
            switch (type) {
            case READ:
                super.setMode(value ? super.getMode() | TGREAD : super.getMode() & ~TGREAD);
                return true;
            case WRITE:
                super.setMode(value ? super.getMode() | TGWRITE : super.getMode() & ~TGWRITE);
                return true;
            case EXECUTE:
                super.setMode(value ? super.getMode() | TGEXEC : super.getMode() & ~TGEXEC);
                return true;
            }
            break;
        case OTHER:
            switch (type) {
            case READ:
                super.setMode(value ? super.getMode() | TOREAD : super.getMode() & ~TOREAD);
                return true;
            case WRITE:
                super.setMode(value ? super.getMode() | TOWRITE : super.getMode() & ~TOWRITE);
                return true;
            case EXECUTE:
                super.setMode(value ? super.getMode() | TOEXEC : super.getMode() & ~TOEXEC);
                return true;
            }
        }
        return false;
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
