/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.file;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author  Christian Schlichtherle
 */
public class TApplicationTest {

    TApplication<RuntimeException> instance;

    @Before
    public void setUp() {
        instance = new TestApplication();
    }

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
        protected int work(String[] args) {
            return args.length;
        }
    } // TestApplication
}