/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.file;

import de.schlichtherle.truezip.socket.IOPool;
import de.schlichtherle.truezip.socket.spi.IOPoolService;
import javax.annotation.concurrent.Immutable;

/**
 * Contains {@link TempFilePool#INSTANCE}.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
public final class TempFilePoolService extends IOPoolService {

    @Override
    public IOPool<?> get() {
        return TempFilePool.INSTANCE;
    }
}
