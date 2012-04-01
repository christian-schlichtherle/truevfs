/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.io;

import de.truezip.kernel.util.ExceptionBuilderTestSuite;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * @param  <X> the type of the assembled exception.
 * @author Christian Schlichtherle
 */
public abstract class SequentialIOExceptionBuilderTestSuite<
        X extends SequentialIOException>
extends ExceptionBuilderTestSuite<
        SequentialIOExceptionBuilder<IOException, X>,
        IOException,
        X> {

    protected SequentialIOExceptionBuilderTestSuite(Class<X> clazz) {
        super(clazz);
    }

    @Override
    protected final IOException newInput() {
        return new IOException("test");
    }

    @Test
    public void testIdentity() throws X {
        assertSequence(new Identity());
    }

    @Test
    public void testAppearance() throws X {
        assertSequence(new Appearance());
    }

    @Test
    public void testPriority() throws X {
        assertSequence(new Priority());
    }

    @SuppressWarnings({ "unchecked", "element-type-mismatch" })
    private void assertSequence(final Mapper mapper) throws X {
        final IOException warning = new IOException("warning");
        final IOException failure = new IOException("failure");
        builder.warn(warning);
        final SequentialIOException ex1 = builder.fail(failure);
        builder.check();
        assertTrue(clazz.isInstance(ex1));
        final SequentialIOException ex2 = mapper.map(ex1);
        assertTrue(clazz.isInstance(ex2));
        final Set<IOException> causes = new HashSet<>(Arrays.asList(warning, failure));
        assertTrue(causes.remove(ex2.getCause()));
        assertTrue(causes.remove(ex2.getPredecessor().getCause()));
        assertTrue(causes.isEmpty());
        assertNull(ex2.getPredecessor().getPredecessor());
    }

    @Test
    public void testFailIdempotence() {
        final X ex = builder.fail(newInput());
        assertSame(ex, builder.fail(ex));
    }

    @Test
    public void testWarnIdempotence() {
        builder.warn(newInput());
        try {
            builder.check();
            fail();
        } catch (final SequentialIOException ex) {
            assertTrue(clazz.isInstance(ex));
            builder.warn(ex);
            try {
                builder.check();
                fail();
            } catch (final SequentialIOException ex2) {
                assertSame(ex, ex2);
            }
        }
    }

    private interface Mapper {
        SequentialIOException map(SequentialIOException ex);
    }

    private class Identity implements Mapper {
        @Override
        public SequentialIOException map(SequentialIOException ex) {
            return ex;
        }
    }

    private final class Appearance implements Mapper {
        @Override
        public SequentialIOException map(SequentialIOException ex) {
            return ex.sortAppearance();
        }
    }

    private final class Priority implements Mapper {
        @Override
        public SequentialIOException map(SequentialIOException ex) {
            return ex.sortPriority();
        }
    }
}
