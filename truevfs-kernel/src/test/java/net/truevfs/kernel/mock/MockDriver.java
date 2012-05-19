/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.mock;

import net.truevfs.kernel.FsController;
import net.truevfs.kernel.FsDriver;
import net.truevfs.kernel.FsManager;
import net.truevfs.kernel.FsModel;
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
        assert null == parent
                ? null == model.getParent()
                : parent.getModel().equals(model.getParent());
        return new MockController(model, parent, null);
    }
}