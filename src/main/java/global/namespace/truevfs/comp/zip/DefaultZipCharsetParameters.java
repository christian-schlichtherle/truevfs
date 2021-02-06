/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.zip;

import java.nio.charset.Charset;
import java.util.Objects;

/**
 * Default implementation of {@link ZipCharsetParameters}.
 * 
 * @author  Christian Schlichtherle
 */
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