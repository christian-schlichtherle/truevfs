/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.file;

import de.schlichtherle.truezip.fs.FsController;
import de.schlichtherle.truezip.fs.FsDriver;
import de.schlichtherle.truezip.fs.FsModel;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;

/**
 * A file system driver for the FILE scheme.
 * 
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
public final class FileDriver extends FsDriver {

    @Override
    public FsController<?>
    newController(FsModel model, @CheckForNull FsController<?> parent) {
        assert null == model.getParent()
                ? null == parent
                : model.getParent().equals(parent.getModel());
        if (null != parent)
            throw new IllegalArgumentException();
        return new FileController(model);
    }
}
