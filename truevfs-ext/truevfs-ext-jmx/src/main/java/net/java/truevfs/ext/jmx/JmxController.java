/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx;

import javax.inject.Provider;
import net.java.truevfs.comp.inst.InstrumentingController;
import net.java.truevfs.kernel.spec.FsController;

/**
 * @author Christian Schlichtherle
 */
public abstract class JmxController
extends InstrumentingController<JmxMediator>
implements JmxColleague, Provider<JmxStatisticsKind> {

    JmxController(JmxMediator director, FsController controller) {
        super(director, controller);
    }

    @Override
    public void start() {
    }
}
