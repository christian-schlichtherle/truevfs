/*
 * Copyright (C) 2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.zip;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.nio.charset.Charset;
import net.jcip.annotations.ThreadSafe;

/**
 * Default implementation of {@link ZipCharsetParameters}.
 * 
 * @since   TrueZIP 7.3
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@DefaultAnnotation(NonNull.class)
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
