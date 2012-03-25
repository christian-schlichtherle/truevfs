/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.rof;

import java.io.File;
import java.io.IOException;

/**
 * @author  Christian Schlichtherle
 */
public final class ChannelReadOnlyFileIT extends ReadOnlyFileTestSuite {

    @Override
    protected ReadOnlyFile newReadOnlyFile(File file) throws IOException {
        return new ChannelReadOnlyFile(file);
    }
}