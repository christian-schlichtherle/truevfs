/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.extension.jmxjul;

import java.util.Objects;
import javax.annotation.concurrent.Immutable;
import net.truevfs.kernel.FsDecoratingModel;
import net.truevfs.kernel.FsModel;

/**
 * @author Christian Schlichtherle
 */
@Immutable
public abstract class InstrumentingModel extends FsDecoratingModel<FsModel> {

    protected final InstrumentingDirector<?> director;

    @SuppressWarnings("LeakingThisInConstructor")
    protected InstrumentingModel(
            final InstrumentingDirector<?> director,
            final FsModel model) {
        super(model);
        this.director = Objects.requireNonNull(director);
    }
}
