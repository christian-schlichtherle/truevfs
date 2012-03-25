/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.io;

import de.truezip.kernel.rof.ReadOnlyFile;
import de.truezip.kernel.util.Pool;
import java.io.IOException;

/**
 * A pool with a single read only file provided to its constructor.
 *
 * @author  Christian Schlichtherle
 */
final class SingleReadOnlyFilePool implements Pool<ReadOnlyFile, IOException> {
    final ReadOnlyFile rof;

    SingleReadOnlyFilePool(final ReadOnlyFile rof) {
        this.rof = rof;
    }

    @Override
    public ReadOnlyFile allocate() {
        return rof;
    }

    @Override
    public void release(ReadOnlyFile rof) {
        assert this.rof == rof;
    }
}