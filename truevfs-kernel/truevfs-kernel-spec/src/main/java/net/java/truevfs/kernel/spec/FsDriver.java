/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec;

import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;

/**
 * An abstract factory for components required to access a file system.
 * <p>
 * Subclasses should be immutable.
 * 
 * @see    FsMetaDriver
 * @author Christian Schlichtherle
 */
@Immutable
public abstract class FsDriver {

    /**
     * Returns {@code true} iff this is an archive driver, i.e. if file systems
     * of this type must be a member of a parent file system.
     * <p>
     * The implementation in the class {@link FsDriver} returns {@code false}.
     * 
     * @return {@code true} iff this is an archive driver, i.e. if file systems
     *         of this type must be a member of a parent file system.
     */
    public boolean isArchiveDriver() {
        return false;
    }

    /**
     * Returns a new thread-safe file system controller for the mount point of
     * the given file system model.
     * <p>
     * When called, you may assert the following precondition:
     * <pre>{@code
     * assert null == parent
     *         ? null == model.getParent()
     *         : parent.getModel().equals(model.getParent())
     * }</pre>
     *
     * @param  manager the manager of the new file system controller.
     * @param  model the file system model.
     * @param  parent the nullable parent file system controller.
     * @return A new thread-safe file system controller for the mount point of
     *         the given file system model.
     * @see    FsMetaDriver#newController
     */
    public abstract FsController newController(
            FsManager manager,
            FsModel model,
            @CheckForNull FsController parent);

    /**
     * Returns {@code this == obj}.
     * 
     * @param obj the object to compare
     * @return {@code this == obj}.
     */
    @Override
    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    public final boolean equals(Object obj) { return this == obj; }

    /**
     * Returns {@code System.identityHashCode(this)}.
     * 
     * @return {@code System.identityHashCode(this)}.
     */
    @Override
    public final int hashCode() { return System.identityHashCode(this); }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return String.format("%s[archiveDriver=%b]",
                getClass().getName(),
                isArchiveDriver());
    }
}
