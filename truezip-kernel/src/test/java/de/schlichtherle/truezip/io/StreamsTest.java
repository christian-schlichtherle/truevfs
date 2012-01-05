/*
 * Copyright (C) 2004-2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public class StreamsTest {
    private byte[] data;
    private TestInputStream in;
    private TestOutputStream out;

    @Before
    public void setUp() {
        data = new byte[3 * Streams.BUFFER_SIZE];
        new Random().nextBytes(data);
        in = new TestInputStream(data);
        out = new TestOutputStream(data.length);
    }

    @Test
    public void testCat() throws Exception {
        Thread.currentThread().interrupt();
        Streams.cat(in, out);
        assertTrue(Thread.interrupted());
        assertFalse(in.closed);
        assertFalse(out.closed);
        assertArrayEquals(data, out.toByteArray());
    }

    @Test
    public void testCopy() throws Exception {
        Thread.currentThread().interrupt();
        Streams.copy(in, out);
        assertTrue(Thread.interrupted());
        assertTrue(in.closed);
        assertTrue(out.closed);
        assertArrayEquals(data, out.toByteArray());
    }

    private static final class TestInputStream extends ByteArrayInputStream {
        boolean closed;

        TestInputStream(final byte[] data) {
            super(data);
        }

        @Override
        public void close() throws IOException {
            super.close();
            closed = true;
        }
    }

    private static final class TestOutputStream extends ByteArrayOutputStream {
        boolean closed;

        TestOutputStream(final int initialCapacity) {
            super(initialCapacity);
        }

        @Override
        public void close() throws IOException {
            super.close();
            closed = true;
        }
    }
}
