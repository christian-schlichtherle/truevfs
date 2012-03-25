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
 * @param  <C> the type of the cause exceptions.
 * @param  <X> the type of the assembled exceptions.
 * @author Christian Schlichtherle
 */
public abstract class ExceptionBuilderTestSuite<
        B extends ExceptionBuilder<C, X>,
        C extends Exception,
        X extends Exception> {

    protected final Class<X> clazz;

    protected B builder;

    protected ExceptionBuilderTestSuite(final Class<X> clazz) {
        if (null == (this.clazz = clazz))
            throw new NullPointerException();        
    }

    protected abstract B newBuilder();

    protected abstract C newCause();

    @Before
    public void setUp() {
        if (null == (builder = newBuilder()))
            throw new NullPointerException();
    }

    @Test
    public void testCheck() throws X {
        builder.check();
    }

    @Test
    public void testFailThenCheck() throws X {
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
    public void testWarnThenCheck() throws X {
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
