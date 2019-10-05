/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.tardriver;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Use this class to workaround issues with decorating objects which have
 * delegates which ignore calls to {@link #close()} once it has failed before.
 *
 * @author Christian Schlichtherle
 */
public final class FixedBufferedOutputStream extends BufferedOutputStream {

    private boolean closed;

    public FixedBufferedOutputStream(OutputStream out, int size) {
        super(out, size);
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            out.close(); // enable recovery
        } else {
            closed = true;
            super.close();
        }
    }
}
