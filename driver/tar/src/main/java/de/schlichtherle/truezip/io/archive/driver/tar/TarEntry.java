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
package de.schlichtherle.truezip.io.archive.driver.tar;

import de.schlichtherle.truezip.io.archive.ArchiveEntry;
import java.io.File;

import static de.schlichtherle.truezip.io.entry.Entry.Access.WRITE;
import static de.schlichtherle.truezip.io.entry.Entry.Size.DATA;
import static de.schlichtherle.truezip.io.entry.Entry.Type.DIRECTORY;
import static de.schlichtherle.truezip.io.entry.Entry.Type.FILE;

/**
 * An entry in a TAR archive which implements the {@code ArchiveEntry}
 * interface.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class TarEntry
extends      org.apache.tools.tar.TarEntry
implements   ArchiveEntry {

    public TarEntry(final String entryName) {
        super(entryName, true);
        // Fix super class constructor.
        super.setModTime(Long.MIN_VALUE);
        super.setSize(UNKNOWN);
        super.setUserName(System.getProperty("user.name", ""));
    }

    public TarEntry(
            final String name,
            final org.apache.tools.tar.TarEntry template) {
        super(name, true);
        super.setMode(template.getMode());
        super.setModTime(template.getModTime());
        super.setSize(template.getSize());
        super.setUserId(template.getUserId());
        super.setUserName(template.getUserName());
        super.setGroupId(template.getGroupId());
        super.setGroupName(template.getGroupName());
    }

    public TarEntry(
            final org.apache.tools.tar.TarEntry template,
            final File file) {
        super(file);
        super.setName(template.getName());
        super.setMode(template.getMode());
        super.setModTime(template.getModTime());
        super.setSize(template.getSize());
        super.setUserId(template.getUserId());
        super.setUserName(template.getUserName());
        super.setGroupId(template.getGroupId());
        super.setGroupName(template.getGroupName());
    }

    @Override
    public Type getType() {
        return isDirectory() ? DIRECTORY : FILE;
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

    /** Returns {@link #getName()}. */
    @Override
    public final String toString() {
        return getName();
    }
}
