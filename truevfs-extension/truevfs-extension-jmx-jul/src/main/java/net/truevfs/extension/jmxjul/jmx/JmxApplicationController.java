/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.extension.jmxjul.jmx;

import net.truevfs.kernel.FsController;

/**
 * @author  Christian Schlichtherle
 */
final class JmxApplicationController extends JmxController {

    JmxApplicationController(JmxDirector director, FsController<?> controller) {
        super(director, controller);
    }

    @Override
    JmxIoStatistics getIOStatistics() {
        return director.getApplicationIOStatistics();
    }
}
