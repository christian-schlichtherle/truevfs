/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.jmx;

import javax.annotation.concurrent.ThreadSafe;
import net.java.truevfs.comp.inst.InstrumentingController;
import net.java.truevfs.kernel.spec.FsController;

/**
 * A controller for a {@linkplain FsController file system controller}.
 * 
 * @param  <M> the type of the JMX mediator.
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class JmxController<M extends JmxMediator<M>>
extends InstrumentingController<M> implements JmxColleague {

    public JmxController(M director, FsController controller) {
        super(director, controller);
    }

    @Override
    public void start() { }
}
