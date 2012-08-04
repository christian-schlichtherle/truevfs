/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.extension.jmx;

import net.java.truevfs.kernel.spec.FsController;

/**
 * @author Christian Schlichtherle
 */
final class JmxApplicationController extends JmxController {

    JmxApplicationController(JmxDirector director, FsController controller) {
        super(director, controller);
    }

    @Override
    JmxIoStatistics getIOStatistics() {
        return director.getApplicationIoStatistics();
    }
}
