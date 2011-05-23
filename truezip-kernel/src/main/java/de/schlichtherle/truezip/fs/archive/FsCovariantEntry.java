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
package de.schlichtherle.truezip.fs.archive;

import static de.schlichtherle.truezip.entry.Entry.Type.*;
import de.schlichtherle.truezip.entry.EntryFactory;
import de.schlichtherle.truezip.fs.FsEntry;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.CharConversionException;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import net.jcip.annotations.NotThreadSafe;

/**
 * A covariant file system entry maintains a map of
 * {@link FsArchiveEntry archive entries} and uses its
 * {@link #setKey(Type) key} property to determine the archive entry
 * in the map to which it forwards the requests for most other properties.
 * 
 * @param   <E> The type of the mapped archive entries.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
@DefaultAnnotation(NonNull.class)
public final class FsCovariantEntry<E extends FsArchiveEntry>
extends FsEntry
implements Cloneable {

    private final String name;
    private Type type;
    private EnumMap<Type, E> map = new EnumMap<Type, E>(Type.class);
    private LinkedHashSet<String> members;

    /** Constructs a new covariant file system entry. */
    public FsCovariantEntry(final String name) {
        if (null == name)
            throw new NullPointerException();
        this.name = name;
    }

    /**
     * Returns a deep clone of this covariant file system entry.
     * 
     * @param  factory the archive entry factory to use for cloning the
     *         mapped archive entries.
     * @return A deep clone of this covariant file system entry.
     */
    @SuppressWarnings("unchecked")
    public FsCovariantEntry<E> clone(EntryFactory<E> factory) {
        final FsCovariantEntry<E> that;
        try {
            that = (FsCovariantEntry<E>) super.clone();
        } catch (CloneNotSupportedException ex) {
            throw new AssertionError(ex);
        }
        final Map<Type, E> thatMap = that.map = new EnumMap<Type, E>(Type.class);
        try {
            for (final Map.Entry<Type, E> entry : this.map.entrySet()) {
                final FsArchiveEntry delegate = entry.getValue();
                thatMap.put(entry.getKey(),
                            factory.newEntry(   delegate.getName(),
                                                delegate.getType(),
                                                delegate));
            }
        } catch (CharConversionException ex) {
            throw new AssertionError(ex);
        }
        final LinkedHashSet<String> members = this.members;
        if (null != members)
            that.members = (LinkedHashSet<String>) members.clone();
        return that;
    }

    @Override
    public String getName() {
        return name;
    }

    public @Nullable Type getKey() {
        return this.type;
    }

    /**
     * Selects the type of file system entry to which most property requests
     * shall get forwarded to.
     * If the given type is {@code null} or no file system entry of its type
     * exists in this map, then any property request will fail with a
     * {@link NullPointerException}.
     * 
     * @param type the type of file system entry to forward most property
     *        requests to.
     */
    public void setKey(final @Nullable Type type) {
        this.type = type;
    }

    /**
     * Maps the given type to the given entry.
     * As a side effect, the {@link #getKey() key} property is set to the given
     * type.
     * 
     * @param type the type.
     * @param entry the entry.
     * @return The previously mapped entry.
     */
    public @Nullable E putEntry(Type type, E entry) {
        return map.put(this.type = type, entry);
    }

    public @Nullable E removeEntry(Type type) {
        return map.remove(type);
    }

    public @CheckForNull E getEntry(Type type) {
        return map.get(type);
    }

    /**
     * Returns the archive entry for the key type.
     *
     * @return the archive entry for the key type.
     */
    public @CheckForNull E getEntry() {
        return map.get(this.type);
    }

    public Collection<E> getEntries() {
        return map.values();
    }

    @Override
    public Set<Type> getTypes() {
        return map.keySet();
    }

    @Override
    public boolean isType(Type type) {
        return map.containsKey(type);
    }

    @Override
    public long getSize(Size type) {
        return map.get(this.type).getSize(type);
    }

    public boolean setSize(Size type, long value) {
        return map.get(this.type).setSize(type, value);
    }

    @Override
    public long getTime(Access type) {
        return map.get(this.type).getTime(type);
    }

    public boolean setTime(Access type, long value) {
        return map.get(this.type).setTime(type, value);
    }

    @Override
    public @Nullable Set<String> getMembers() {
        if (!map.containsKey(DIRECTORY))
            return this.members = null;
        final Set<String> members = this.members;
        return null != members
                ? members
                : (this.members = new LinkedHashSet<String>());
    }

    /**
     * Adds the given base path to the set of members of this directory
     * if and only if this covariant file system entry implements a directory.
     *
     * @param  member The base path of the member to add.
     * @return Whether the member has been added or an equal member was
     *         already present in the directory.
     * @throws NullPointerException if this covariant file system entry does
     *         not implement a directory.
     */
    public boolean add(String member) {
        return getMembers().add(member);
    }

    /**
     * Removes the given base path from the set of members of this
     * directory
     * if and only if this covariant file system entry implements a directory.
     *
     * @param  member The base path of the member to remove.
     * @return Whether the member has been removed or no equal member was
     *         present in the directory.
     * @throws NullPointerException if this covariant file system entry does
     *         not implement a directory.
     */
    public boolean remove(String member) {
        return getMembers().remove(member);
    }
}
