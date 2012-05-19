/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.extension.jmxjul.jmx;

import net.truevfs.extension.jmxjul.InstrumentingController;
import net.truevfs.kernel.FsController;
import net.truevfs.kernel.FsModel;

/**
 * @author  Christian Schlichtherle
 */
abstract class JmxController
extends InstrumentingController<JmxDirector> {

    JmxController(FsController<? extends FsModel> controller, JmxDirector director) {
        super(controller, director);
    }

    abstract JmxIOStatistics getIOStatistics();
}