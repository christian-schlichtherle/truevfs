/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.kernel.api;

import global.namespace.truevfs.comp.cio.Entry;
import global.namespace.truevfs.comp.shed.BitField;
import global.namespace.truevfs.comp.shed.ImplementationsShouldExtend;

import javax.annotation.Nullable;
import java.util.Set;

/**
 * A file system node is an entry which can implement multiple entry types and
 * list directory members.
 *
 * @author Christian Schlichtherle
 */
@ImplementationsShouldExtend(FsAbstractNode.class)
public interface FsNode extends Entry {

    /**
     * Returns a string representation of the
     * {@link FsNodeName file system node name}.
     *
     * @return A string representation of the
     *         {@link FsNodeName file system node name}.
     */
    @Override
    String getName();

    /**
     * Returns a bit field of types implemented by this file system node.
     * <p>
     * Some file system types allow a node to implement multiple types.
     * For example, a ZIP or TAR file may contain an archive entry with the
     * name {@code foo} and a directory archive entry with the name {@code foo/}.
     * Yes, this is strange, but sh*t happens!
     * In this case then, a virtual file system should collapse this into one
     * file system node which returns {@code true} for both
     * {@code isType(FILE)} and {@code isType(DIRECTORY)}.
     *
     * @return A bit field of types implemented by this file system node.
     */
    BitField<Type> getTypes();

    /**
     * Returns {@code true} if and only if this file system node implements
     * the given type.
     *
     * @param  type the type to test.
     * @return {@code true} if and only if this file system node implements
     *         the given type.
     * @see    #getTypes()
     */
    boolean isType(Type type);

    /**
     * Returns a set of strings with the base names of the members of this
     * directory node or {@code null} if and only if this is not a directory
     * node.
     * Whether or not modifying the returned set is supported and the effect
     * on the file system is implementation specific.
     *
     * @return A set of strings with the base names of the members of this
     *         directory node or {@code null} if and only if this is not a
     *         directory node.
     */
    @Nullable Set<String> getMembers();
}
