/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx;

import javax.annotation.concurrent.ThreadSafe;
import javax.management.ObjectName;
import net.java.truecommons.shed.BitField;
import net.java.truevfs.comp.inst.InstrumentingManager;
import net.java.truevfs.comp.jmx.JmxManagerMXBean;
import static net.java.truevfs.comp.jmx.JmxUtils.*;
import net.java.truevfs.kernel.spec.FsManager;
import net.java.truevfs.kernel.spec.FsSyncException;
import net.java.truevfs.kernel.spec.FsSyncOption;
import net.java.truevfs.kernel.spec.FsSyncOptions;
import net.java.truevfs.kernel.spec.sl.FsManagerLocator;

/**
 * A controller for a {@linkplain FsManager file system manager}.
 * 
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class JmxManager
extends InstrumentingManager<JmxMediator> implements JmxColleague {

    public JmxManager(JmxMediator mediator, FsManager manager) {
        super(mediator, manager);
    }

    private ObjectName name() {
        return mediator.nameBuilder(FsManager.class).get();
    }

    protected JmxManagerMXBean newView() {
        return new JmxManagerView(this);
    }

    @Override
    public void start() {
        register(name(), newView());
        mediator.startAllStatistics();
    }

    @Override
    public void sync(BitField<FsSyncOption> options) throws FsSyncException {
        final long start = System.nanoTime();
        super.sync(options);
        mediator.logSync(System.nanoTime() - start);
        mediator.rotateAllStatistics();
    }

    void sync() throws FsSyncException {
        FsManagerLocator.SINGLETON.get().sync(FsSyncOptions.NONE);
    }
}
