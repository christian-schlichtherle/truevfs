/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.inst;

import java.util.Objects;
import javax.annotation.concurrent.ThreadSafe;
import net.java.truevfs.kernel.spec.*;

/**
 * @param  <M> the type of the mediator.
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class InstrumentingManager<M extends Mediator<M>>
extends FsDecoratingManager {

    protected final M mediator;

    public InstrumentingManager(
            final M mediator,
            final FsManager manager) {
        super(manager);
        this.mediator = Objects.requireNonNull(mediator);
    }

    @Override
    public FsController controller(
            FsCompositeDriver driver,
            FsMountPoint mountPoint) {
        return mediator.instrument(this,
                manager.controller(
                    mediator.instrument(this, driver),
                    mountPoint));
    }
}
