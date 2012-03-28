/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.fs;

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
