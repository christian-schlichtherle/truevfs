/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.shed;

import java.util.Collections;
import java.util.Iterator;
import org.junit.Test;

/**
 * @author Christian Schlichtherle
 */
public class StreamTest {

    @Test
    public void testStream() {
        TestContainer container = new TestContainer();
        try (TestStream stream = container.stream()) {
            for (Object object : stream) {
                throw new AssertionError(object);
            }
        }
    }

    private class TestContainer {
        TestStream stream() { return new TestStream(); }
    } // TestContainer

    private class TestStream implements Stream<Object> {
        @Override
        public Iterator<Object> iterator() {
            return Collections.emptyIterator();
        }

        @Override
        public void close() {
        }
    } // TestStream
}
