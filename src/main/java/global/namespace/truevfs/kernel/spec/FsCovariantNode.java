/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.kernel.spec;

import global.namespace.truevfs.comp.shed.BitField;

import javax.annotation.Nullable;
import java.util.*;

import static global.namespace.truevfs.comp.cio.Entry.Type.DIRECTORY;

/**
 * A covariant file system node maintains a map of
 * {@link FsArchiveEntry archive entries} and uses its
 * {@link #getKey() key} property to determine the archive entry in the map to
 * which it forwards calls to {@link #getEntry()}, {@link #getSize(Size)},
 * {@link #getTime(Access)} et al.
 *
 * @param  <E> the type of the mapped archive entries.
 * @author Christian Schlichtherle
 */
public final class FsCovariantNode<E extends FsArchiveEntry>
extends FsAbstractNode implements Cloneable {

    private final String name;
    private EnumMap<Type, E> map = new EnumMap<>(Type.class);
    private @Nullable Type key;
    private @Nullable LinkedHashSet<String> members;

    /**
     * Constructs a new covariant file system node with the given path.
     *
     * @param path the file system path.
     */
    public FsCovariantNode(String path) { name = path.toString(); } // check NPE

    /**
     * Returns a deep clone of this covariant file system node.
     *
     * @param  driver the archive driver to use for cloning the mapped archive
     *         entries.
     * @return A deep clone of this covariant file system node.
     */
    @SuppressWarnings("unchecked")
    public FsCovariantNode<E> clone(final FsArchiveDriver<E> driver) {
        final FsCovariantNode<E> clone;
        try {
            clone = (FsCovariantNode<E>) super.clone();
        } catch (CloneNotSupportedException ex) {
            throw new AssertionError(ex);
        }
        final Map<Type, E> cloneMap = clone.map = new EnumMap<>(Type.class);
        for (final Map.Entry<Type, E> mapping : this.map.entrySet()) {
            final FsArchiveEntry entry = mapping.getValue();
            cloneMap.put(mapping.getKey(),
                    driver.newEntry(entry.getName(), entry.getType(), entry));
        }
        final LinkedHashSet<String> members = this.members;
        if (null != members)
            clone.members = (LinkedHashSet<String>) members.clone();
        return clone;
    }

    @Override
    public String getName() { return name; }

    /**
     * Returns {@code true} if and only if the name of this covariant node
     * identifies it as a root node.
     *
     * @return {@code true} if and only if the name of this covariant node
     *         identifies it as a root node.
     */
    public boolean isRoot() { return name.isEmpty(); }

    /**
     * Returns the type of the file system node to which calls to
     * {@link #getEntry()}, {@link #getSize(Size)},
     * {@link #getTime(Access)} et al shall get forwarded.
     * <p>
     * Note that an arbitrary property value may get returned:
     * The initial value is {@code null} and even if it's not {@code null},
     * no node of this type needs to be mapped.
     *
     * @return the type of the file system node to which calls to
     *         {@link #getEntry()}, {@link #getSize(Size)},
     *         {@link #getTime(Access)} et al shall get forwarded.
     */
    public @Nullable Type getKey() { return key; }

    /**
     * Selects the type of the file system node to which calls to
     * {@link #getEntry()}, {@link #getSize(Size)},
     * {@link #getTime(Access)} et al shall get forwarded.
     * If the given type is {@code null} or no file system node of this type
     * is mappeed, then any subsequent call to these methods will fail with a
     * {@link NullPointerException}.
     *
     * @param key the type of the file system node to which calls to
     *        {@link #getEntry()}, {@link #getSize(Size)},
     *        {@link #getTime(Access)} et al shall get forwarded.
     */
    public void setKey(@Nullable Type key) { this.key = key; }

    /**
     * Maps the given type to the given archive entry.
     * As a side effect, the {@link #getKey() key} property is set to the given
     * type.
     *
     * @param type the type to map.
     * @param entry the archive entry to map.
     * @return The previously mapped archive entry.
     */
    public @Nullable E put(Type type, E entry) {
        return map.put(key = type, entry);
    }

    /**
     * Removes the archive entry for the given type from the map.
     *
     * @param type the type to remove.
     * @return The previously mapped archive entry.
     */
    public @Nullable E remove(Type type) { return map.remove(type); }

    /**
     * Returns the archive entry for the given type.
     *
     * @param type the type of the archive entry to lookup.
     * @return The archive entry for the given type.
     */
    public @Nullable E get(Type type) { return map.get(type); }

    /**
     * Returns the archive entry mapped for the {@link #getKey() key} property.
     *
     * @return the archive entry mapped for the {@link #getKey() key} property.
     */
    public @Nullable E getEntry() { return map.get(key); }

    /**
     * A collection of the mapped entries.
     * This is a bidirectional view: Any change is reflected in the map and
     * vice versa.
     *
     * @return a collection of the mapped entries
     */
    public Collection<E> getEntries() { return map.values(); }

    /**
     * Returns a set of the mapped types.
     * This is a bidirectional view: Any change is reflected in the map and
     * vice versa.
     */
    @Override
    public BitField<Type> getTypes() { return BitField.copyOf(map.keySet()); }

    /**
     * Returns {@code true} if and only if there is an archive entry mapped for
     * the given type.
     */
    @Override
    public boolean isType(Type type) { return map.containsKey(type); }

    /**
     * Returns the size mapped for the {@link #getKey() key} property.
     */
    @Override
    public long getSize(Size type) {
        return DIRECTORY == key ? UNKNOWN : map.get(key).getSize(type);
    }

    /**
     * Returns the access time mapped for the {@link #getKey() key} property.
     */
    @Override
    public long getTime(Access type) { return map.get(key).getTime(type); }

    /**
     * Returns the permission mapped for the {@link #getKey() key} property.
     */
    @Override
    public Boolean isPermitted(Access type, Entity entity) {
        return map.get(key).isPermitted(type, entity);
    }

    /**
     * Returns a set of the members of this directory or {@code null} if and
     * only if there is no directory archive entry mapped.
     * This is a bidirectional view: Any change is reflected in the set and
     * vice versa.
     */
    @Override
    public @Nullable Set<String> getMembers() {
        if (!isType(DIRECTORY)) return members = null;
        final Set<String> m = members;
        return null != m ? m : (members = new LinkedHashSet<>());
    }

    /**
     * Adds the given base path to the set of members of this directory
     * if and only if this covariant file system node implements a directory.
     *
     * @param  member The base path of the member to add.
     * @return Whether the member has been added or an equal member was
     *         already present in the directory.
     * @throws NullPointerException if this covariant file system node does
     *         not implement a directory.
     */
    public boolean add(String member) { return getMembers().add(member); }

    /**
     * Removes the given base path from the set of members of this directory
     * if and only if this covariant file system node implements a directory.
     *
     * @param  member The base path of the member to remove.
     * @return Whether the member has been removed or no equal member was
     *         present in the directory.
     * @throws NullPointerException if this covariant file system node does
     *         not implement a directory.
     */
    public boolean remove(String member) { return getMembers().remove(member); }
}
