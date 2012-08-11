/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx;

import javax.annotation.concurrent.ThreadSafe;
import net.java.truevfs.comp.inst.InstrumentingController;
import net.java.truevfs.comp.jmx.JmxColleague;
import net.java.truevfs.kernel.spec.FsController;

/**
 * A controller for a {@linkplain FsController file system controller}.
 * 
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class JmxController
extends InstrumentingController<JmxMediator> implements JmxColleague {

    JmxController(JmxMediator director, FsController controller) {
        super(director, controller);
    }

    @Override
    public void start() { }
}
