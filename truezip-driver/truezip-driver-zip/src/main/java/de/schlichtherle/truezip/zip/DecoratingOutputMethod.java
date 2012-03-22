/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.zip;

import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipException;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * A decorator for output methods.
 * <p>
 * Implementations cannot be thread-safe.
 *
 * @since   TrueZIP 7.3
 * @author  Christian Schlichtherle
 */
@NotThreadSafe
abstract class DecoratingOutputMethod implements OutputMethod {
    final OutputMethod delegate;

    DecoratingOutputMethod(final OutputMethod processor) {
        assert null != processor;
        this.delegate = processor;
    }

    @Override
    public void init(ZipEntry entry) throws ZipException {
        delegate.init(entry);
    }

    @Override
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    public OutputStream start() throws IOException {
        return delegate.start();
    }

    @Override
    public void finish() throws IOException {
        delegate.finish();
    }
}