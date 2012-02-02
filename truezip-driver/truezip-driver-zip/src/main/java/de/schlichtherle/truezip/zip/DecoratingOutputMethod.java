/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.zip;

import java.io.IOException;
import java.io.OutputStream;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * A decorator for output methods.
 * <p>
 * Implementations cannot be thread-safe.
 *
 * @since   TrueZIP 7.3
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
abstract class DecoratingOutputMethod
implements OutputMethod {

    final OutputMethod delegate;

    DecoratingOutputMethod(final OutputMethod processor) {
        assert null != processor;
        this.delegate = processor;
    }

    @Override
    public void init(ZipEntry entry) throws IOException {
        delegate.init(entry);
    }

    @Override
    public OutputStream start() throws IOException {
        return delegate.start();
    }

    @Override
    public void finish() throws IOException {
        delegate.finish();
    }
}
