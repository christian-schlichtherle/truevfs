/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.kernel.api.mock;

import global.namespace.truevfs.kernel.api.FsController;
import global.namespace.truevfs.kernel.api.FsDriver;
import global.namespace.truevfs.kernel.api.FsManager;
import global.namespace.truevfs.kernel.api.FsModel;

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
