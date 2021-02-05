/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.commons.zip;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

/**
 * An inflater input stream which uses a custom {@link Inflater} and provides
 * access to it.
 *
 * @author Christian Schlichtherle
 */
final class ZipInflaterInputStream extends InflaterInputStream {

    ZipInflaterInputStream(InputStream in, int size) {
        super(in, new Inflater(true), size);
    }

    Inflater getInflater() {
        return inf;
    }

    @Override
    public void close() throws IOException {
        super.close();
        inf.end();
    }
}
