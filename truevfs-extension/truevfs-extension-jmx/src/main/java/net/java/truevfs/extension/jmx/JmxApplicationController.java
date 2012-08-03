/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.extension.jmx;

import net.java.truevfs.kernel.spec.FsController;
import net.java.truevfs.kernel.spec.FsModel;

/**
 * @author Christian Schlichtherle
 */
final class JmxApplicationController<M extends FsModel>
extends JmxController<M> {

    JmxApplicationController(JmxDirector director, FsController<M> controller) {
        super(director, controller);
    }

    @Override
    JmxIoStatistics getIOStatistics() {
        return director.getApplicationIoStatistics();
    }
}
