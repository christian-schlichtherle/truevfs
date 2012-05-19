/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.zip.io;

import java.nio.charset.Charset;
import java.util.Objects;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Default implementation of {@link ZipCharsetParameters}.
 * 
 * @author  Christian Schlichtherle
 */
@ThreadSafe
class DefaultZipCharsetParameters implements ZipCharsetParameters {

    private final Charset charset;

    DefaultZipCharsetParameters(final Charset charset) {
        this.charset = Objects.requireNonNull(charset);
    }

    @Override
    public Charset getCharset() {
        return charset;
    }
}