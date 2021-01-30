/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec;

import javax.annotation.concurrent.ThreadSafe;

/**
 * An abstract decorator for a file system controller.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
public abstract class FsDecoratingController extends FsAbstractController implements FsDelegatingController {

    /** The decorated file system controller. */
    protected final FsController controller;

    protected FsDecoratingController(final FsController controller) {
        super(controller.getModel());
        this.controller = controller;
    }

    @Override
    public FsController getController() {
        return controller;
    }

    @Override
    public FsController getParent() {
        return controller.getParent();
    }

    @Override
    public String toString() {
        return String.format("%s@%x[controller=%s]",
                getClass().getName(),
                hashCode(),
                controller);
    }
}
