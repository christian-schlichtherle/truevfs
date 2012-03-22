/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.inst.jmx;

import de.schlichtherle.truezip.fs.FsController;
import de.schlichtherle.truezip.fs.inst.InstrumentingController;

/**
 * @author  Christian Schlichtherle
 */
abstract class JmxController
extends InstrumentingController<JmxDirector> {

    JmxController(FsController<?> controller, JmxDirector director) {
        super(controller, director);
    }

    abstract JmxIOStatistics getIOStatistics();
}