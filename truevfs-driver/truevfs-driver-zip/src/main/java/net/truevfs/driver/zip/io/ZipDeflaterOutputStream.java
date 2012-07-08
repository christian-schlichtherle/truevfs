/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.zip.io;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.Deflater;
import static java.util.zip.Deflater.DEFAULT_COMPRESSION;
import java.util.zip.DeflaterOutputStream;

/**
 * A deflater output stream which uses a custom {@link Deflater} and provides
 * access to it.
 * 
 * @author Christian Schlichtherle
 */
final class ZipDeflaterOutputStream extends DeflaterOutputStream {

    ZipDeflaterOutputStream(OutputStream out, int level, int size) {
        super(out, new Deflater(DEFAULT_COMPRESSION, true), size);
        def.setLevel(level);
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