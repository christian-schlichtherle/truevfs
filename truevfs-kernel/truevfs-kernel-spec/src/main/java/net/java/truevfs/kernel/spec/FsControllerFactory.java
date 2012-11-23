/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec;

import javax.annotation.Nullable;

/**
 * A factory for {@linkplain FsController file system controllers}.
 * <p>
 * Implementations need to be safe for multithreading.
 *
 * @param  <Context> The type of the calling context.
 * @since  TrueVFS 0.10
 * @author Christian Schlichtherle
 */
public interface FsControllerFactory<Context> {

    /**
     * Returns a new thread-safe file system controller for the mount point of
     * the given file system model.
     * This is a pure function without side effects.
     * <p>
     * When called, you may assert the following precondition:
     * <pre>{@code
     * assert null == parent
     *         ? null == model.getParent()
     *         : parent.getModel().equals(model.getParent())
     * }</pre>
     *
     * @param  context the calling context.
     * @param  model the file system model.
     * @param  parent the nullable parent file system controller.
     * @return A new thread-safe file system controller for the mount point of
     *         the given file system model.
     */
    FsController newController(
            Context context,
            FsModel model,
            @Nullable FsController parent);
}
