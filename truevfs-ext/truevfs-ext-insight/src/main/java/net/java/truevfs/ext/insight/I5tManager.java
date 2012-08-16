/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.insight;

import javax.annotation.concurrent.ThreadSafe;
import net.java.truecommons.shed.BitField;
import net.java.truevfs.comp.jmx.JmxManager;
import net.java.truevfs.kernel.spec.FsManager;
import net.java.truevfs.kernel.spec.FsSyncException;
import net.java.truevfs.kernel.spec.FsSyncOption;

/**
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class I5tManager extends JmxManager<I5tMediator> {

    I5tManager(I5tMediator mediator, FsManager manager) {
        super(mediator, manager);
    }

    @Override
    public void start() {
        super.start();
        mediator.startAllStats(this);
    }

    @Override
    public void sync(BitField<FsSyncOption> options) throws FsSyncException {
        final long start = System.nanoTime();
        super.sync(options);
        mediator.logSync(System.nanoTime() - start);
        mediator.rotateAllStats(this);
    }
}