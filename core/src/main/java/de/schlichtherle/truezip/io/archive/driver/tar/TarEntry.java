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

import de.schlichtherle.truezip.io.archive.entry.ArchiveEntry;
import java.io.File;

import static de.schlichtherle.truezip.io.archive.entry.CommonEntry.Type.DIRECTORY;
import static de.schlichtherle.truezip.io.archive.entry.CommonEntry.Type.FILE;

/**
 * An entry in a TAR archive which implements the {@code ArchiveEntry}
 * interface.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class TarEntry
extends     org.apache.tools.tar.TarEntry
implements  ArchiveEntry {

    public TarEntry(final String entryName) {
        super(entryName, true);
        // Fix super class constructor.
        super.setModTime(Long.MIN_VALUE);
        super.setSize(UNKNOWN);
        super.setUserName(System.getProperty("user.name", ""));
    }

    public TarEntry(
            final org.apache.tools.tar.TarEntry blueprint) {
        super(blueprint.getName(), true);
        super.setMode(blueprint.getMode());
        super.setModTime(blueprint.getModTime());
        super.setSize(blueprint.getSize());
        super.setUserId(blueprint.getUserId());
        super.setUserName(blueprint.getUserName());
        super.setGroupId(blueprint.getGroupId());
        super.setGroupName(blueprint.getGroupName());
    }

    public TarEntry(
            final org.apache.tools.tar.TarEntry blueprint,
            final File file) {
        super(file);
        super.setName(blueprint.getName());
        super.setMode(blueprint.getMode());
        super.setModTime(blueprint.getModTime());
        super.setSize(blueprint.getSize());
        super.setUserId(blueprint.getUserId());
        super.setUserName(blueprint.getUserName());
        super.setGroupId(blueprint.getGroupId());
        super.setGroupName(blueprint.getGroupName());
    }

    @Override
    public Type getType() {
        return isDirectory() ? DIRECTORY : FILE;
    }

    @Override
    public long getTime() {
        long time = super.getModTime().getTime();
        return time >= 0 ? time : UNKNOWN;
    }

    @Override
    public void setTime(long time) {
        super.setModTime(time);
    }

    /** Returns {@link #getName()}. */
    @Override
    public final String toString() {
        return getName();
    }
}
