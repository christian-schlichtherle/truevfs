/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.file.oio;

import de.truezip.kernel.cio.IOPool;
import de.truezip.kernel.spi.IOPoolService;
import javax.annotation.concurrent.Immutable;

/**
 * Contains {@link TempFilePool#INSTANCE}.
 *
 * @author Christian Schlichtherle
 */
@Immutable
public final class TempFilePoolService extends IOPoolService {

    @Override
    public IOPool<?> get() {
        return TempFilePool.INSTANCE;
    }
}
