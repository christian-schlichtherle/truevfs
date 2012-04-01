/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.util;

import de.truezip.kernel.util.PriorityExceptionBuilderTest.TestException;
import java.util.Arrays;
import java.util.Comparator;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * @author Christian Schlichtherle
 */
public final class PriorityExceptionBuilderTest
extends ExceptionBuilderTestSuite<  PriorityExceptionBuilder<TestException>,
                                    TestException,
                                    TestException> {

    public PriorityExceptionBuilderTest() {
        super(TestException.class);
    }

    @Override
    protected PriorityExceptionBuilder<TestException> newBuilder() {
        return new PriorityExceptionBuilder<>(TestComparator.INSTANCE);
    }

    @Override
    protected TestException newInput() {
        return new TestException(0);
    }

    @Test
    public void testFailIdempotence() {
        final TestException ex = builder.fail(newInput());
        assertSame(ex, builder.fail(ex));
    }

    @Test
    public void testWarnIdempotence() {
        builder.warn(newInput());
        try {
            builder.check();
            fail();
        } catch (final TestException ex) {
            assertTrue(clazz.isInstance(ex));
            builder.warn(ex);
            try {
                builder.check();
                fail();
            } catch (final TestException ex2) {
                assertSame(ex, ex2);
            }
        }
    }

    @Test
    public void testPriority() throws TestException {
        for (final TestException[] params : new TestException[][] {
            // { $expected, $input1, $input2, ... },
            {   new TestException(0, new WarningException(1)),
                new WarningException(1),
                new TestException(0), },
            {   new TestException(0, new WarningException(1)),
                new TestException(0),
                new WarningException(1), },
            {   new TestException(0, new WarningException(1), new WarningException(2)),
                new WarningException(1),
                new WarningException(2),
                new TestException(0), },
            {   new TestException(0, new WarningException(1), new WarningException(2)),
                new WarningException(1),
                new TestException(0),
                new WarningException(2), },
            {   new TestException(0, new WarningException(1), new WarningException(2)),
                new TestException(0),
                new WarningException(1),
                new WarningException(2), },
        }) {
            final TestException expected = params[0];
            int i = 1;
            while (i < params.length - 1)
                builder.warn(params[i++]);
            final TestException assembly = builder.fail(params[i]);
            builder.check();
            assertEquals(expected, assembly);
        }
    }

    @SuppressWarnings("serial")
    static class TestException extends Exception {
        final int id;

        TestException(final int id, final TestException... suppressed) {
            this.id = id;
            for (TestException ex : suppressed)
                super.addSuppressed(ex);
        }

        int getPriority() {
            return 0;
        }

        @Override
        public synchronized Throwable fillInStackTrace() {
            return this;
        }

        public boolean equals(Object that) {
            if (this == that)
                return true;
            if (!(that instanceof TestException))
                return false;
            final TestException other = (TestException) that;
            return id == other.id
                    && getPriority() == other.getPriority()
                    && Arrays.equals(getSuppressed(), other.getSuppressed());
        }

        public int hashCode() {
            throw new UnsupportedOperationException();
        }
    } // TestException

    @SuppressWarnings("serial")
    static class WarningException extends TestException {
        WarningException(final int id, final TestException... suppressed) {
            super(id, suppressed);
        }

        @Override
        int getPriority() {
            return -1;
        }
    } // WarningException

    private static final class TestComparator
    implements Comparator<TestException> {
        static final TestComparator INSTANCE = new TestComparator();

        @Override
        public int compare(TestException o1, TestException o2) {
            return o1.getPriority() - o2.getPriority();
        }
    }
}
