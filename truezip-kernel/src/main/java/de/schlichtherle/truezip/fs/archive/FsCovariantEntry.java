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
import de.schlichtherle.truezip.fs.FsEntry;
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
 * {@link #setKey(Entry.Type) key} property to determine the archive entry
 * in the map to which it forwards calls to {@link #getEntry()},
 * {@link #getSize(Size)} and {@link #getTime(Access)}.
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
    private EnumMap<Type, E> map = new EnumMap<Type, E>(Type.class);
    private @Nullable Type type;
    private @Nullable LinkedHashSet<String> members;

    /**
     * Constructs a new covariant file system entry with given path.
     * 
     * @param path the file system path.
     */
    public FsCovariantEntry(final String path) {
        if (null == path)
            throw new NullPointerException();
        this.name = path;
    }

    /**
     * Returns a deep clone of this covariant file system entry.
     * 
     * @param  driver the archive driver to use for cloning the mapped archive
     *         entries.
     * @return A deep clone of this covariant file system entry.
     */
    @SuppressWarnings("unchecked")
    public FsCovariantEntry<E> clone(FsArchiveDriver<E> driver) {
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
                            driver.newEntry(delegate.getName(),
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

    /**
     * Returns the type of the file system entry to which calls to
     * {@link #getEntry()}, {@link #getSize(Size)} and
     * {@link #getTime(Access)} shall get forwarded.
     * <p>
     * Note that an arbitrary property value may get returned:
     * The initial value is {@code null} and even if it's not {@code null},
     * no entry of this type needs to be mapped.
     * 
     * @return the type of the file system entry to which calls to
     *         {@link #getEntry()}, {@link #getSize(Size)} and
     *         {@link #getTime(Access)} shall get forwarded.
     */
    public @Nullable Type getKey() {
        return this.type;
    }

    /**
     * Selects the type of the file system entry to which calls to
     * {@link #getEntry()}, {@link #getSize(Size)} and
     * {@link #getTime(Access)} shall get forwarded.
     * If the given type is {@code null} or no file system entry of this type
     * is mappeed, then any subsequent call to these methods will fail with a
     * {@link NullPointerException}.
     * 
     * @param type the type of the file system entry to which calls to
     *        {@link #getEntry()}, {@link #getSize(Size)} and
     *        {@link #getTime(Access)} shall get forwarded.
     */
    public void setKey(final @Nullable Type type) {
        this.type = type;
    }

    /**
     * Maps the given type to the given entry.
     * As a side effect, the {@link #getKey() key} property is set to the given
     * type.
     * 
     * @param type the type to map.
     * @param entry the entry to map.
     * @return The previously mapped entry.
     */
    public @Nullable E putEntry(Type type, E entry) {
        return map.put(this.type = type, entry);
    }

    /**
     * Removes the entry for the given type from the map.
     * 
     * @param type the type to remove.
     * @return The previously mapped entry.
     */
    public @Nullable E removeEntry(Type type) {
        return map.remove(type);
    }

    /**
     * Returns the entry for the given type.
     * 
     * @param type the type of the entry to lookup.
     * @return The entry for the given type.
     */
    public @Nullable E getEntry(Type type) {
        return map.get(type);
    }

    /**
     * Returns the archive entry mapped for the {@link #getKey() key} property.
     *
     * @return the archive entry mapped for the {@link #getKey() key} property.
     */
    public @Nullable E getEntry() {
        return map.get(this.type);
    }

    /**
     * A collection of the mapped entries.
     * This is a bidirectional view: Any change is reflected in the map and
     * vice versa.
     * 
     * @return a collection of the mapped entries
     */
    public Collection<E> getEntries() {
        return map.values();
    }

    /**
     * A set of the mapped types.
     * This is a bidirectional view: Any change is reflected in the map and
     * vice versa.
     * 
     * @return a set of the mapped types
     */
    @Override
    public Set<Type> getTypes() {
        return map.keySet();
    }

    /**
     * Returns {@code true} if there is an entry mapped for the given type.
     * 
     * @param type the type to lookup.
     * @return {@code true} if there is an entry mapped for the given type.
     */
    @Override
    public boolean isType(Type type) {
        return map.containsKey(type);
    }

    /**
     * Returns the size mapped for the {@link #getKey() key} property.
     *
     * @param type the size type to lookup.
     * @return the size mapped for the {@link #getKey() key} property.
     */
    @Override
    public long getSize(Size type) {
        return map.get(this.type).getSize(type);
    }

    /**
     * Returns the access time mapped for the {@link #getKey() key} property.
     *
     * @param type the access time type to lookup.
     * @return the access time mapped for the {@link #getKey() key} property.
     */
    @Override
    public long getTime(Access type) {
        return map.get(this.type).getTime(type);
    }

    /**
     * Returns a set of the members of this directory or {@code null} if and
     * only if there is no directory entry mapped.
     * This is a bidirectional view: Any change is reflected in the set and
     * vice versa.
     * 
     * @return A set of the members of this directory or {@code null} if and
     *         only if there is no directory entry mapped.
     */
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
