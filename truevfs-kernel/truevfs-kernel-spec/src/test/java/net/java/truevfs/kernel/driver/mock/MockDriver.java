/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.driver.mock;

import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;
import net.java.truevfs.kernel.spec.*;

/**
 * @author Christian Schlichtherle
 */
@Immutable
public final class MockDriver extends FsDriver {

    @Override
    public FsController newController(
            final FsManager context,
            final FsModel model,
            final @CheckForNull FsController parent) {
        return new MockController(model, parent, null);
    }
}
