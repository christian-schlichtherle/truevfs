/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.extension.jmxjul.jmx;

import de.truezip.kernel.fs.FsController;
import de.truezip.extension.jmxjul.InstrumentingController;

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