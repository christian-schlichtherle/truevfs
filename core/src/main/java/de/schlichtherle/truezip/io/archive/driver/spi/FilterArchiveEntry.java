/*
 * Copyright 2007-2010 Schlichtherle IT Services
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

package de.schlichtherle.truezip.io.archive.driver.spi;

import de.schlichtherle.truezip.io.archive.driver.ArchiveEntry;
import de.schlichtherle.truezip.io.archive.filesystem.ArchiveEntryMetaData;

/**
 * A decorator for archive entries.
 *
 * @param <AE> The type of the decorated archive entry.
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class FilterArchiveEntry<AE extends ArchiveEntry>
implements ArchiveEntry {

    /** The decorated archive entry - may be {@code null}. */
    protected AE entry;

    protected FilterArchiveEntry(final AE entry) {
        this.entry = entry;
    }

    @Override
    public String getName() {
        return entry.getName();
    }

    @Override
    public Type getType() {
        return entry.getType();
    }

    @Override
    public long getSize() {
        return entry.getSize();
    }

    @Override
    public void setSize(long size) {
        entry.setSize(size);
    }

    @Override
    public long getTime() {
        return entry.getTime();
    }

    @Override
    public void setTime(long time) {
        entry.setTime(time);
    }

    @Override
    public ArchiveEntryMetaData getMetaData() {
        return entry.getMetaData();
    }

    @Override
    public void setMetaData(ArchiveEntryMetaData metaData) {
        entry.setMetaData(metaData);
    }
}
