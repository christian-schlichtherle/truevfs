/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.mock;

import de.truezip.kernel.FsController;
import de.truezip.kernel.FsDriver;
import de.truezip.kernel.FsManager;
import de.truezip.kernel.FsModel;
import javax.annotation.concurrent.Immutable;

/**
 * @author  Christian Schlichtherle
 */
@Immutable
public final class MockDriver extends FsDriver {

    @Override
    public FsController<?>
    controller(  FsManager manager,
                    FsModel model,
                    FsController<?> parent) {
        assert null == parent
                ? null == model.getParent()
                : parent.getModel().equals(model.getParent());
        return new MockController(model, parent, null);
    }
}