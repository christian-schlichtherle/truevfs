/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.util;

import java.util.Objects;
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
        B extends ExceptionBuilder<? super I, ? extends O>,
        I extends Exception,
        O extends Exception> {

    protected final Class<O> clazz;

    protected B builder;

    protected ExceptionBuilderTestSuite(final Class<O> clazz) {
        this.clazz = Objects.requireNonNull(clazz);
    }

    protected abstract B newBuilder();

    protected abstract I newInput();

    @Before
    public void setUp() {
        builder = Objects.requireNonNull(newBuilder());
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

        assertNotNull(builder.fail(newInput()));
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
            builder.warn(newInput());
            builder.check();
            fail();
        } catch (final Exception expected) {
            assertTrue(clazz.isInstance(expected));
            builder.check();
        }
    }
}
