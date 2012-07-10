/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.mock;

import de.schlichtherle.truezip.fs.FsController;
import de.schlichtherle.truezip.fs.FsDriver;
import de.schlichtherle.truezip.fs.FsModel;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;

/**
 * @author Christian Schlichtherle
 */
@Immutable
public final class MockDriver extends FsDriver {

    @Override
    public FsController<?> newController(
            final FsModel model,
            final @CheckForNull FsController<?> parent) {
        return new MockController(model, parent, null);
    }
}
