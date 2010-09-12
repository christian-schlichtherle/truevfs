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

package de.schlichtherle.truezip.io.archive.entry;

/**
 * A decorator for archive entries.
 *
 * @param <AE> The type of the decorated archive entry.
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class FilterArchiveEntry<AE extends ArchiveEntry>
implements ArchiveEntry {

    /** The decorated nullable archive entry. */
    protected AE target;

    protected FilterArchiveEntry(final AE entry) {
        this.target = entry;
    }

    @Override
    public String getName() {
        return target.getName();
    }

    @Override
    public Type getType() {
        return target.getType();
    }

    @Override
    public long getSize() {
        return target.getSize();
    }

    @Override
    public void setSize(long size) {
        target.setSize(size);
    }

    @Override
    public long getTime() {
        return target.getTime();
    }

    @Override
    public void setTime(long time) {
        target.setTime(time);
    }
}
