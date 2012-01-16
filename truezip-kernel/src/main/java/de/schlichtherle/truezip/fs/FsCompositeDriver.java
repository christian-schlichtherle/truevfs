/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.ServiceConfigurationError;

/**
 * Queries the scheme of the mount point of the given file system model in
 * order to lookup the appropriate file system driver which is then used to
 * create the requested thread-safe file system controller.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
public interface FsCompositeDriver {

    /**
     * Returns a new thread-safe file system controller for the mount point of
     * the given file system model and parent file system controller.
     * The file system controller is created by using a
     * {@link FsDriver file system driver} which is looked up by querying the
     * scheme of the mount point of the given file system model with the
     * expression {@code model.getMountPoint().getScheme()}.
     * <p>
     * When called, the following expression is a precondition:
     * {@code
            null == model.getParent()
                    ? null == parent
                    : model.getParent().equals(parent.getModel())
     * }
     *
     * @param  model the file system model.
     * @param  parent the nullable parent file system controller.
     * @return A new thread-safe file system controller for the given mount
     *         point and parent file system controller.
     * @throws ServiceConfigurationError if no appropriate file system driver
     *         can get found for the scheme of the given mount point.
     * @see    FsDriver#newController
     */
    FsController<?>
    newController(FsModel model, @Nullable FsController<?> parent);
}
