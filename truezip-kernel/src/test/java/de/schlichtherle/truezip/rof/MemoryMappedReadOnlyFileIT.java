/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.rof;

import java.io.File;
import java.io.IOException;

/**
 * @author  Christian Schlichtherle
 */
public final class MemoryMappedReadOnlyFileIT extends ReadOnlyFileTestSuite {
    @Override
    protected ReadOnlyFile newReadOnlyFile(File file) throws IOException {
        return new MemoryMappedReadOnlyFile(file);
    }
}