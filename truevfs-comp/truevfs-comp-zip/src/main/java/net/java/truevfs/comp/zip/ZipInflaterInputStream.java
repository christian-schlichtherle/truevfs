/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.zip;

import edu.umd.cs.findbugs.annotations.CleanupObligation;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import edu.umd.cs.findbugs.annotations.DischargesObligation;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import javax.annotation.WillCloseWhenClosed;

/**
 * An inflater input stream which uses a custom {@link Inflater} and provides
 * access to it.
 *
 * @author Christian Schlichtherle
 */
@CleanupObligation
final class ZipInflaterInputStream extends InflaterInputStream {

    @CreatesObligation
    ZipInflaterInputStream(@WillCloseWhenClosed InputStream in, int size) {
        super(in, new Inflater(true), size);
    }

    Inflater getInflater() {
        return inf;
    }

    @Override
    @DischargesObligation
    public void close() throws IOException {
        super.close();
        inf.end();
    }
}
