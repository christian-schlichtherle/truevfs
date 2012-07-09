/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.ext.jmx;

import net.truevfs.ext.inst.InstrumentingController;
import net.truevfs.kernel.spec.FsController;
import net.truevfs.kernel.spec.FsModel;

/**
 * @author  Christian Schlichtherle
 */
abstract class JmxController
extends InstrumentingController<JmxDirector> {

    JmxController(JmxDirector director, FsController<? extends FsModel> controller) {
        super(director, controller);
    }

    abstract JmxIoStatistics getIOStatistics();
}
