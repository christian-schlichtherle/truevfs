/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.util;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 * @param  <B> the type of the exception builder.
 * @param  <I> the type of the input exceptions.
 * @param  <O> the type of the assembled (output) exceptions.
 * @author Christian Schlichtherle
 */
public abstract class ExceptionBuilderTestSuite<
        B extends ExceptionBuilder<I, O>,
        I extends Exception,
        O extends Exception> {

    protected final Class<O> clazz;

    protected B builder;

    protected ExceptionBuilderTestSuite(final Class<O> clazz) {
        if (null == (this.clazz = clazz))
            throw new NullPointerException();        
    }

    protected abstract B newBuilder();

    protected abstract I newCause();

    @Before
    public void setUp() {
        if (null == (builder = newBuilder()))
            throw new NullPointerException();
    }

    @Test
    public void testCheck() throws O {
        builder.check();
    }

    @Test
    public void testFailThenCheck() throws O {
        try {
            builder.fail(null);
            fail();
        } catch (final RuntimeException expected) {
            builder.check();
        }

        assertNotNull(builder.fail(newCause()));
        builder.check();
    }

    @Test
    public void testWarnThenCheck() throws O {
        try {
            builder.warn(null);
            fail();
        } catch (final RuntimeException expected) {
            builder.check();
        }

        try {
            builder.warn(newCause());
            builder.check();
            fail();
        } catch (final Exception expected) {
            assertTrue(clazz.isInstance(expected));
            builder.check();
        }
    }
}
