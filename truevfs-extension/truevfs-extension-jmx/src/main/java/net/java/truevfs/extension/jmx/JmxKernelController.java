/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.extension.jmx;

import net.java.truevfs.component.instrumentation.InstrumentingController;
import net.java.truevfs.kernel.spec.FsController;

/**
 * @author Christian Schlichtherle
 */
final class JmxKernelController
extends InstrumentingController<JmxDirector> implements JmxWithIoStatistics {

    JmxKernelController(JmxDirector director, FsController controller) {
        super(director, controller);
    }

    @Override
    public JmxIoStatistics getStats() {
        return director.getKernelStats();
    }
}
