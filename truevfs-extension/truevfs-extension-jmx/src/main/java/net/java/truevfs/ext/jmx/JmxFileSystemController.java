/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx;

import net.java.truevfs.comp.jmx.JmxController;
import net.java.truevfs.comp.inst.InstrumentingController;
import net.java.truevfs.kernel.spec.FsController;

/**
 * @author Christian Schlichtherle
 */
public abstract class JmxFileSystemController
extends InstrumentingController<JmxDirector>
implements JmxController, JmxStatisticsProvider {

    JmxFileSystemController(JmxDirector director, FsController controller) {
        super(director, controller);
    }

    @Override
    public void init() {
    }
}
