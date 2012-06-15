/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.util;

/**
 * @author Christian Schlichtherle
 */
public final class SuppressedExceptionBuilderTest
extends PriorityExceptionBuilderTest {

    @Override
    protected PriorityExceptionBuilder<TestException> newBuilder() {
        return new SuppressedExceptionBuilder<>();
    }

    @Override
    public void testPriority() {
    }
}
