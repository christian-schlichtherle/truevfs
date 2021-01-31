/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec.mock;

import net.java.truevfs.kernel.spec.FsController;
import net.java.truevfs.kernel.spec.FsDriver;
import net.java.truevfs.kernel.spec.FsManager;
import net.java.truevfs.kernel.spec.FsModel;

import javax.annotation.CheckForNull;

/**
 * @author Christian Schlichtherle
 */
public final class MockDriver extends FsDriver {

    @Override
    public FsController newController(
            final FsManager context,
            final FsModel model,
            final @CheckForNull FsController parent) {
        return new MockController(model, parent, null);
    }
}
