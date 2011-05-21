/*
 * Copyright (C) 2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.fs.archive.mock;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.fs.archive.FsArchiveEntry;
import de.schlichtherle.truezip.socket.IOPool;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.EnumMap;
import java.util.EnumSet;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
public final class MockArchiveEntry implements FsArchiveEntry {

    private final String name;
    private final Type type;
    private final EnumMap<Size, Long>
            sizes = new EnumMap<Size, Long>(Size.class);
    private final EnumMap<Access, Long>
            times = new EnumMap<Access, Long>(Access.class);
    @Nullable IOPool.Entry<?> io;

    public MockArchiveEntry(final String name,
                            final Type type,
                            final @CheckForNull Entry template) {
        assert null != name;
        assert null != type;
        this.name = name;
        this.type = type;
        if (null != template) {
            for (final Size size : EnumSet.allOf(Size.class)) {
                final long value = template.getSize(size);
                if (UNKNOWN != value)
                    sizes.put(size, value);
            }
            for (final Access access : EnumSet.allOf(Access.class)) {
                final long value = template.getTime(access);
                if (UNKNOWN != value)
                    times.put(access, value);
            }
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public long getSize(final Size type) {
        final Long size = sizes.get(type);
        return null != size ? size : UNKNOWN;
    }

    @Override
    public boolean setSize(final Size type, final long value) {
        sizes.put(type, value);
        return true;
    }

    @Override
    public long getTime(final Access type) {
        final Long time = times.get(type);
        return null != time ? time : UNKNOWN;
    }

    @Override
    public boolean setTime(final Access type, final long value) {
        times.put(type, value);
        return true;
    }
}
