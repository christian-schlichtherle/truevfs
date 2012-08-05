/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.component.instrumentation;

import java.io.InputStream;
import java.util.Objects;
import net.java.truecommons.io.DecoratingInputStream;

/**
 * @param  <D> the type of the director.
 * @see    InstrumentingOutputStream
 * @author Christian Schlichtherle
 */
public class InstrumentingInputStream<D extends Director<D>>
extends DecoratingInputStream {
    protected final D director;

    public InstrumentingInputStream(
            final D director,
            final InputStream in) {
        super(in);
        this.director = Objects.requireNonNull(director);
    }
}
