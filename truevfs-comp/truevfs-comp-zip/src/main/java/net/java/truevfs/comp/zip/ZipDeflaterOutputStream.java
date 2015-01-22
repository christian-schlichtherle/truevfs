/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.zip;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

/**
 * A deflater output stream which uses a custom {@link Deflater} and provides
 * access to it.
 * 
 * @author Christian Schlichtherle
 */
final class ZipDeflaterOutputStream extends DeflaterOutputStream {

    ZipDeflaterOutputStream(OutputStream out, int level, int size) {
        super(out, new Deflater(level, true), size);
    }

    Deflater getDeflater() {
        return def;
    }

    @Override
    public void close() throws IOException {
        assert false : "This method should never get called by the current implementation.";
        def.end();
        super.close();
    }
}
