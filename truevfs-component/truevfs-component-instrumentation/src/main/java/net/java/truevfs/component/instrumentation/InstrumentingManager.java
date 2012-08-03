/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.component.instrumentation;

import java.util.Objects;
import javax.annotation.concurrent.Immutable;
import net.java.truevfs.kernel.spec.*;

/**
 * @param  <D> the type of the instrumenting director.
 * @author Christian Schlichtherle
 */
@Immutable
public class InstrumentingManager<D extends InstrumentingDirector<D>>
extends FsDecoratingManager<FsManager> {
    protected final D director;

    public InstrumentingManager(
            final D director,
            final FsManager manager) {
        super(manager);
        this.director = Objects.requireNonNull(director);
    }

    @Override
    public FsController<? extends FsModel> controller(FsCompositeDriver driver, FsMountPoint mountPoint) {
        return director.instrument(manager.controller(director.instrument(driver, this), mountPoint), this);
    }
}
