/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel;

import de.truezip.kernel.FsSyncException;
import de.truezip.kernel.FsSyncExceptionBuilder;
import de.truezip.kernel.io.SequentialIOExceptionBuilderTestSuite;

/**
 * @author Christian Schlichtherle
 */
public final class FsSyncExceptionBuilderTest
extends SequentialIOExceptionBuilderTestSuite<FsSyncException> {

    public FsSyncExceptionBuilderTest() {
        super(FsSyncException.class);
    }

    @Override
    protected FsSyncExceptionBuilder newBuilder() {
        return new FsSyncExceptionBuilder();
    }
}
