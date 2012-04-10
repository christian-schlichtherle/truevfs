/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.io;

import java.nio.charset.Charset;
import javax.annotation.concurrent.ThreadSafe;

/**
 * The default implementation of {@link ZipFileParameters}.
 *
 * @author  Christian Schlichtherle
 */
@ThreadSafe
final class DefaultZipFileParameters
extends DefaultZipCharsetParameters
implements ZipFileParameters<ZipEntry> {

    private final boolean preambled, postambled;

    DefaultZipFileParameters(
            final Charset charset,
            final boolean preambled,
            final boolean postambled) {
        super(charset);
        this.preambled = preambled;
        this.postambled = postambled;
    }

    @Override
    public boolean getPreambled() {
        return preambled;
    }

    @Override
    public boolean getPostambled() {
        return postambled;
    }

    @Override
    public ZipEntry newEntry(String name) {
        return new ZipEntry(name);
    }
}