/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.file;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
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
