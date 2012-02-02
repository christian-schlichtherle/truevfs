/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * An abstract factory for components required to access a file system.
 * <p>
 * Sub-classes must be thread-safe and should be immutable.
 * 
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
public abstract class FsDriver {

    /**
     * Returns a new thread-safe file system controller for the mount point of
     * the given file system model and parent file system controller.
     * <p>
     * When called, you may safely assume the following precondition:
     * <pre>{@code
     * assert null == model.getParent()
     *         ? null == parent
     *         : model.getParent().equals(parent.getModel())
     * }</pre>
     *
     * @param  model the file system model.
     * @param  parent the nullable parent file system controller.
     * @return A new thread-safe file system controller for the given mount
     *         point and parent file system controller.
     * @see    FsCompositeDriver#newController
     */
    public abstract FsController<?>
    newController(FsModel model, @Nullable FsController<?> parent);

    /**
     * Returns {@code true} iff this file system driver implements a federated
     * file system type, i.e. if the type of file system must be a member of a
     * parent file system.
     * <p>
     * The implementation in the class {@link FsDriver} returns {@code false}.
     * 
     * @return {@code true} iff the type of the file system implemented by this
     *         file system driver is federated, i.e. must be a member of a
     *         parent file system.
     */
    public boolean isFederated() {
        return false;
    }

    /**
     * Returns a priority to help the file system driver service locator.
     * The greater number wins!
     * 
     * @return {@code 0}, as by the implementation in the class
     *         {@link FsDriver}.
     */
    public int getPriority() {
        return 0;
    }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return new StringBuilder()
                .append(getClass().getName())
                .append("[federated=")
                .append(isFederated())
                .append(",priority=")
                .append(getPriority())
                .append(']')
                .toString();
    }
}
