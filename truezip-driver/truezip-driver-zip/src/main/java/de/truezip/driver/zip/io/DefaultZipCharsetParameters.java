/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.io;

import java.nio.charset.Charset;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Default implementation of {@link ZipCharsetParameters}.
 * 
 * @since   TrueZIP 7.3
 * @author  Christian Schlichtherle
 */
@ThreadSafe
class DefaultZipCharsetParameters implements ZipCharsetParameters {

    private final Charset charset;

    DefaultZipCharsetParameters(final Charset charset) {
        if (null == charset)
            throw new NullPointerException();
        this.charset = charset;
    }

    @Override
    public Charset getCharset() {
        return charset;
    }
}