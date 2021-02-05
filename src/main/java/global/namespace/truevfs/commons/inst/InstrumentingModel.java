/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.commons.inst;

import global.namespace.truevfs.kernel.api.FsDecoratingModel;
import global.namespace.truevfs.kernel.api.FsModel;

import java.util.Objects;

/**
 * @param  <M> the type of the mediator.
 * @author Christian Schlichtherle
 */
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
