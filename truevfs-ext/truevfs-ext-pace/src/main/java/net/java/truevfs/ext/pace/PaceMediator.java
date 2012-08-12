/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.pace;

import javax.annotation.concurrent.ThreadSafe;
import net.java.truevfs.comp.inst.InstrumentingManager;
import net.java.truevfs.comp.jmx.JmxMediator;
import net.java.truevfs.kernel.spec.FsController;
import net.java.truevfs.kernel.spec.FsManager;

/**
 * A mediator for the instrumentation of the TrueVFS Kernel with a
 * {@linkplain PaceManager pace manager}.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class PaceMediator extends JmxMediator<PaceMediator> {

    static final PaceMediator SINGLETON = new PaceMediator();

    private PaceMediator() { }

    @Override
    public FsManager instrument(FsManager object) {
        return start(new PaceManager(this, object));
    }

    @Override
    public FsController instrument(
            InstrumentingManager<PaceMediator> origin,
            FsController object) {
        return new PaceController((PaceManager) origin, object);
    }
}
