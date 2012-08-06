/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.component.instrumentation;

import java.io.OutputStream;
import java.util.Objects;
import net.java.truecommons.io.DecoratingOutputStream;

/**
 * @param  <D> the type of the director.
 * @see    InstrumentingInputStream
 * @author Christian Schlichtherle
 * @deprecated Unused.
 */
@Deprecated
public class InstrumentingOutputStream<D extends Director<D>>
extends DecoratingOutputStream {
    protected final D director;

    public InstrumentingOutputStream(
            final D director,
            final OutputStream out) {
        super(out);
        this.director = Objects.requireNonNull(director);
    }
}
