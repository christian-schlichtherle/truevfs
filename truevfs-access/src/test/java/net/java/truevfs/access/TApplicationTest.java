/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.access;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * @author  Christian Schlichtherle
 */
public final class TApplicationTest {

    private final TApplication<RuntimeException> instance = new TestApplication();

    @Test
    public void testSetup() {
        instance.setup();
    }

    @Test
    public void testWork() {
        try {
            instance.work(null);
        } catch (NullPointerException expected) {
        }

        assertEquals(0, instance.work(new String[0]));
    }

    private static final class TestApplication
    extends TApplication<RuntimeException> {
        @Override
        protected void setup() { }

        @Override
        protected int work(String[] args) {
            return args.length;
        }
    } // TestApplication
}