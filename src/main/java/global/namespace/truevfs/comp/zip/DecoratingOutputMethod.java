/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.zip;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipException;

/**
 * A decorator for output methods.
 * <p>
 * Implementations cannot be thread-safe.
 *
 * @author  Christian Schlichtherle
 */
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
    public OutputStream start() throws IOException {
        return method.start();
    }

    @Override
    public void finish() throws IOException {
        method.finish();
    }
}