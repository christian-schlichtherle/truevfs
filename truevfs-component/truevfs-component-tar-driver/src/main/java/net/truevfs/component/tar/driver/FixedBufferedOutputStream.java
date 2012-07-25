/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */

package net.truevfs.component.tar.driver;

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
    private boolean ignoreClose;

    public FixedBufferedOutputStream(OutputStream out, int size) {
        super(out, size);
    }

    @Override
    public void close() throws IOException {
        if (!ignoreClose) super.close();
    }

    public boolean getIgnoreClose() {
        return ignoreClose;
    }

    public void setIgnoreClose(final boolean ignoreClose) {
        this.ignoreClose = ignoreClose;
    }
}
