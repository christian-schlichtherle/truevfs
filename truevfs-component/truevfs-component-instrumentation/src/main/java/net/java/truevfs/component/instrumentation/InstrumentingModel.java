/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.component.instrumentation;

import java.util.Objects;
import javax.annotation.concurrent.Immutable;
import net.java.truevfs.kernel.spec.FsDecoratingModel;
import net.java.truevfs.kernel.spec.FsModel;

/**
 * @param  <D> the type of the instrumenting director.
 * @author Christian Schlichtherle
 */
@Immutable
public class InstrumentingModel<D extends InstrumentingDirector<D>>
extends FsDecoratingModel {
    protected final D director;

    public InstrumentingModel(
            final D director,
            final FsModel model) {
        super(model);
        this.director = Objects.requireNonNull(director);
    }
}
