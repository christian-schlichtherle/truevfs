/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.inst.jmx;

import de.schlichtherle.truezip.fs.FsController;

/**
 * @author  Christian Schlichtherle
 */
final class JmxApplicationController extends JmxController {

    JmxApplicationController(FsController<?> controller, JmxDirector director) {
        super(controller, director);
    }

    @Override
    JmxIOStatistics getIOStatistics() {
        return director.getApplicationIOStatistics();
    }
}