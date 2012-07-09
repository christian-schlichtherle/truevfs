/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.zip.core;

import net.truevfs.driver.zip.core.io.DateTimeConverter;
import net.truevfs.driver.zip.core.io.ZipEntry;
import net.truevfs.kernel.spec.FsArchiveEntries;
import net.truevfs.kernel.spec.FsArchiveEntry;
import net.truevfs.kernel.spec.cio.Entry;
import static net.truevfs.kernel.spec.cio.Entry.Access.WRITE;
import static net.truevfs.kernel.spec.cio.Entry.Size.DATA;
import static net.truevfs.kernel.spec.cio.Entry.Type.DIRECTORY;
import static net.truevfs.kernel.spec.cio.Entry.Type.FILE;

/**
 * ZIP archive entries apply the date/time conversion rules as defined by
 * {@link DateTimeConverter#ZIP}.
 *
 * @see    #getDateTimeConverter()
 * @see    ZipDriver
 * @author Christian Schlichtherle
 */
public abstract class AbstractZipDriverEntry extends ZipEntry implements FsArchiveEntry {

    protected AbstractZipDriverEntry(String name) {
        super(name);
        assert invariants();
    }

    protected AbstractZipDriverEntry(String name, ZipEntry template) {
        super(name, template);
        assert invariants();
    }

    private boolean invariants() {
        assert ZipEntry.UNKNOWN == Entry.UNKNOWN;
        return true;
    }

    @Override
    public Type getType() {
        return isDirectory() ? DIRECTORY : FILE;
    }

    @Override
    protected abstract DateTimeConverter getDateTimeConverter();

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

    @Override
    public Boolean isPermitted(Access type, Entity entity) {
        return null;
    }

    @Override
    public boolean setPermitted(Access type, Entity entity, Boolean value) {
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
