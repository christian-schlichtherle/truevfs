/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.archive.tar;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Use this class to workaround issues with decorating objects which have
 * implementations which do not call close() again once it has failed.
 * 
 * @author Christian Schlichtherle
 */
final class FixedBufferedOutputStream extends BufferedOutputStream {
    boolean ignoreClose;

    FixedBufferedOutputStream(OutputStream out, int size) {
        super(out, size);
    }

    @Override
    public void close() throws IOException {
        if (!ignoreClose) super.close();
    }
}
