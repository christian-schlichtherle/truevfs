/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec.mock;

import net.java.truevfs.kernel.spec.FsController;
import net.java.truevfs.kernel.spec.FsDriver;
import net.java.truevfs.kernel.spec.FsManager;
import net.java.truevfs.kernel.spec.FsModel;

import java.util.Optional;

/**
 * @author Christian Schlichtherle
 */
public final class MockDriver extends FsDriver {

    @Override
    public FsController newController(
            FsManager context,
            FsModel model,
            Optional<? extends FsController> parent) {
        assert model.getParent().equals(parent.map(FsController::getModel));
        return new MockController(model, parent, Optional.empty());
    }
}
