/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.zip.io;

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
 * @author  Christian Schlichtherle
 */
@NotThreadSafe
abstract class DecoratingOutputMethod implements OutputMethod {
    final OutputMethod method;

    DecoratingOutputMethod(final OutputMethod processor) {
        assert null != processor;
        this.method = processor;
    }

    @Override
    public void init(ZipEntry entry) throws ZipException {
        method.init(entry);
    }

    @Override
    @CreatesObligation
    public OutputStream start() throws IOException {
        return method.start();
    }

    @Override
    public void finish() throws IOException {
        method.finish();
    }
}