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
 * Subclasses must be thread-safe and should be immutable!
 * 
 * @see    FsCompositeDriver
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
     * the given file system model and nullable parent file system controller.
     * <p>
     * When called, you may assert the following precondition:
     * <pre>{@code
     * assert null == parent
     *         ? null == model.getParent()
     *         : parent.getModel().equals(model.getParent())
     * }</pre>
     *
     * @param  manager the file system manager for the new controller.
     * @param  model the file system model.
     * @param  parent the nullable parent file system controller.
     * @return A new thread-safe file system controller for the given mount
     *         point and nullable parent file system controller.
     * @see    FsCompositeDriver#newController
     */
    public abstract FsController newController(
            FsManager manager,
            FsModel model,
            @CheckForNull FsController parent);

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
