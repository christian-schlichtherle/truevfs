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
 * The default implementation of {@link ZipFileParameters}.
 *
 * @since   TrueZIP 7.3
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@DefaultAnnotation(NonNull.class)
class DefaultZipFileParameters
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
