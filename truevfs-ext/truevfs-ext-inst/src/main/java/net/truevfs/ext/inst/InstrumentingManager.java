/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.ext.inst;

import java.util.Objects;
import javax.annotation.concurrent.Immutable;
import net.truevfs.kernel.spec.*;

/**
 * @author Christian Schlichtherle
 */
@Immutable
public class InstrumentingManager extends FsDecoratingManager<FsManager> {

    protected final InstrumentingDirector<?> director;

    public InstrumentingManager(
            final InstrumentingDirector<?> director,
            final FsManager manager) {
        super(manager);
        this.director = Objects.requireNonNull(director);
    }

    @Override
    public FsController<? extends FsModel> controller(FsCompositeDriver driver, FsMountPoint mountPoint) {
        return director.instrument(manager.controller(director.instrument(driver, this), mountPoint), this);
    }
}
