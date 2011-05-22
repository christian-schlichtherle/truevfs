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
package de.schlichtherle.truezip.fs;

import java.util.EnumMap;
import java.util.Set;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Collections;
import net.jcip.annotations.NotThreadSafe;

/**
 * A covariant file system entry is a map of {@link FsEntry file system entries}
 * which uses its {@link #setKeyType(Type) keyType} property to determine the
 * file system entry to which it forwards the requests for most other
 * properties.
 * 
 * @param   <E> The type of the contained file system entries.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
@DefaultAnnotation(NonNull.class)
public final class FsCovariantEntry<E extends FsEntry>
extends FsEntry {

    private final String name;
    private Type type;
    private final EnumMap<Type, E> map = new EnumMap<Type, E>(Type.class);

    public FsCovariantEntry(final String name) {
        if (null == name)
            throw new NullPointerException();
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    public Type getKeyType() {
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
    public void setKeyType(final Type type) {
        this.type = type;
    }

    public void put(E entry) {
        for (Type type : entry.getTypes())
            put(type, entry);
    }

    public E put(Type type, E entry) {
        return map.put(type, entry);
    }

    public E remove(Type type) {
        return map.remove(type);
    }

    public E get(Type type) {
        return map.get(type);
    }

    public E get() {
        return map.get(this.type);
    }

    @Override
    public Set<Type> getTypes() {
        return Collections.unmodifiableSet(map.keySet());
    }

    @Override
    public boolean isType(Type type) {
        return map.containsKey(type);
    }

    @Override
    public long getSize(Size type) {
        return map.get(this.type).getSize(type);
    }

    @Override
    public long getTime(Access type) {
        return map.get(this.type).getTime(type);
    }

    @Override
    public Set<String> getMembers() {
        return map.get(this.type).getMembers();
    }
}
