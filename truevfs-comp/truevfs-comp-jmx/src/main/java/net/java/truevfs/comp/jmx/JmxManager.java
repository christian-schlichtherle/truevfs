/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.jmx;

import javax.annotation.concurrent.ThreadSafe;
import javax.management.ObjectName;
import net.java.truevfs.comp.inst.InstrumentingManager;
import static net.java.truevfs.comp.jmx.JmxUtils.*;
import net.java.truevfs.kernel.spec.FsManager;
import net.java.truevfs.kernel.spec.FsSyncException;
import net.java.truevfs.kernel.spec.FsSyncOptions;
import net.java.truevfs.kernel.spec.FsSyncWarningException;
import net.java.truevfs.kernel.spec.sl.FsManagerLocator;

/**
 * A controller for a {@linkplain FsManager file system manager}.
 * 
 * @param  <M> the type of the JMX mediator.
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class JmxManager<M extends JmxMediator<M>>
extends InstrumentingManager<M> implements JmxColleague {

    public JmxManager(M mediator, FsManager manager) {
        super(mediator, manager);
    }

    private ObjectName name() {
        return mediator.nameBuilder(FsManager.class).get();
    }

    protected JmxManagerMXBean newView() { return new JmxManagerView<>(this); }

    @Override
    public void start() { register(name(), newView()); }

    public void sync() throws FsSyncWarningException, FsSyncException {
        FsManagerLocator.SINGLETON.get().sync(FsSyncOptions.NONE);
    }
}
