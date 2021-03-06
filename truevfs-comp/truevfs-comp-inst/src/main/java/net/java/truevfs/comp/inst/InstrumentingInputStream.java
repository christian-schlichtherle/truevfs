/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.inst;

import java.io.InputStream;
import java.util.Objects;
import javax.annotation.concurrent.Immutable;
import net.java.truecommons.io.DecoratingInputStream;

/**
 * @param  <M> the type of the mediator.
 * @see    InstrumentingOutputStream
 * @author Christian Schlichtherle
 */
@Immutable
public class InstrumentingInputStream<M extends Mediator<M>>
extends DecoratingInputStream {

    protected final M mediator;

    public InstrumentingInputStream(
            final M mediator,
            final InputStream in) {
        super(in);
        this.mediator = Objects.requireNonNull(mediator);
    }
}
