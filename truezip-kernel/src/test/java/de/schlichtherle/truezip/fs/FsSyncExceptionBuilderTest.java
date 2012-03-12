/*
 * Copyright (C) 2004-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import de.schlichtherle.truezip.io.SequentialIOExceptionBuilderTestSuite;

/**
 * @author Christian Schlichtherle
 */
public class FsSyncExceptionBuilderTest
extends SequentialIOExceptionBuilderTestSuite<FsSyncException> {

    public FsSyncExceptionBuilderTest() {
        super(FsSyncException.class);
    }

    @Override
    protected FsSyncExceptionBuilder newBuilder() {
        return new FsSyncExceptionBuilder();
    }
}
