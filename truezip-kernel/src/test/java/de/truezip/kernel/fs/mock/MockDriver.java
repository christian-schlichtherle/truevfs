/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.fs.mock;

import de.truezip.kernel.fs.FsController;
import de.truezip.kernel.fs.FsDriver;
import de.truezip.kernel.fs.FsManager;
import de.truezip.kernel.fs.FsModel;
import javax.annotation.concurrent.Immutable;

/**
 * @author  Christian Schlichtherle
 */
@Immutable
public final class MockDriver extends FsDriver {

    @Override
    public FsController<?>
    newController(  FsManager manager,
                    FsModel model,
                    FsController<?> parent) {
        assert null == model.getParent()
                ? null == parent
                : model.getParent().equals(parent.getModel());
        return new MockController(model, parent, null);
    }
}