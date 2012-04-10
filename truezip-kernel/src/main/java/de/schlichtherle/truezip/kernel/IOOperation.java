/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.kernel;

import java.io.IOException;
import java.util.concurrent.Callable;
import javax.annotation.Nullable;

/**
 * A callable for I/O operations.
 * 
 * @author Christian Schlichtherle
 */
interface IOOperation<V> extends Callable<V> {

    @Override
    @Nullable V call() throws IOException;
}
