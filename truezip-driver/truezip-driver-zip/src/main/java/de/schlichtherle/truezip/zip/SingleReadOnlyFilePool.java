/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.zip;

import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.util.Pool;
import java.io.IOException;

/**
 * A pool with a single read only file provided to its constructor.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
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
