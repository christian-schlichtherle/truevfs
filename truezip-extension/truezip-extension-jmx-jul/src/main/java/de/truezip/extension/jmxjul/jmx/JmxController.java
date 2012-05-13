/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.extension.jmxjul.jmx;

import de.truezip.extension.jmxjul.InstrumentingController;
import de.truezip.kernel.FsController;
import de.truezip.kernel.FsModel;

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