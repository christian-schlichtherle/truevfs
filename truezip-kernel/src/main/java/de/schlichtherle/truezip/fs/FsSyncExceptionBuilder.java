/*
 * Copyright 2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs;

import de.schlichtherle.truezip.io.SequentialIOException;
import de.schlichtherle.truezip.io.SequentialIOExceptionBuilder;
import java.io.IOException;
import net.jcip.annotations.NotThreadSafe;

/**
 * Assembles a {@link FsSyncException} from one or more {@link IOException}s by
 * {@link SequentialIOException#initPredecessor(SequentialIOException) chaining}
 * them.
 * When the assembly is thrown or returned later, it is sorted by
 * {@link SequentialIOException#sortPriority() priority}.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
public final class FsSyncExceptionBuilder
extends SequentialIOExceptionBuilder<IOException, FsSyncException> {
    public FsSyncExceptionBuilder() {
        super(IOException.class, FsSyncException.class);
    }
}
