/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.inst;

import java.util.Objects;
import javax.annotation.concurrent.ThreadSafe;
import net.java.truevfs.kernel.spec.FsDecoratingModel;
import net.java.truevfs.kernel.spec.FsModel;

/**
 * @param  <D> the type of the director.
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class InstrumentingModel<D extends Director<D>>
extends FsDecoratingModel {
    protected final D director;

    public InstrumentingModel(
            final D director,
            final FsModel model) {
        super(model);
        this.director = Objects.requireNonNull(director);
    }
}
