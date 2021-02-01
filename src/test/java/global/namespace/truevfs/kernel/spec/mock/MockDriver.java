/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.kernel.spec.mock;

import global.namespace.truevfs.kernel.spec.FsController;
import global.namespace.truevfs.kernel.spec.FsDriver;
import global.namespace.truevfs.kernel.spec.FsManager;
import global.namespace.truevfs.kernel.spec.FsModel;

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
