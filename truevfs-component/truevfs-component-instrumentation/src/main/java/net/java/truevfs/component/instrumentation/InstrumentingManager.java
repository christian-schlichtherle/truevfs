/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.component.instrumentation;

import java.util.Objects;
import javax.annotation.concurrent.Immutable;
import net.java.truevfs.kernel.spec.*;

/**
 * @param  <D> the type of the director.
 * @author Christian Schlichtherle
 */
@Immutable
public class InstrumentingManager<D extends Director<D>>
extends FsDecoratingManager {
    protected final D director;

    public InstrumentingManager(
            final D director,
            final FsManager manager) {
        super(manager);
        this.director = Objects.requireNonNull(director);
    }

    @Override
    public FsController controller(
            FsMetaDriver driver,
            FsMountPoint mountPoint) {
        return director.instrument(this,
                manager.controller(
                    director.instrument(this, driver),
                    mountPoint));
    }
}
