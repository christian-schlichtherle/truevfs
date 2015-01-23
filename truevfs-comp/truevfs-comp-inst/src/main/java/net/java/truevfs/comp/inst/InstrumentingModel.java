/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.inst;

import java.util.Objects;
import javax.annotation.concurrent.ThreadSafe;
import net.java.truevfs.kernel.spec.FsDecoratingModel;
import net.java.truevfs.kernel.spec.FsModel;

/**
 * @param  <M> the type of the mediator.
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class InstrumentingModel<M extends Mediator<M>>
extends FsDecoratingModel {

    protected final M mediator;

    public InstrumentingModel(
            final M mediator,
            final FsModel model) {
        super(model);
        this.mediator = Objects.requireNonNull(mediator);
    }
}
