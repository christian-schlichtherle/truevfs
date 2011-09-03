/*
 * Copyright (C) 2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.sample.file.app;

import de.schlichtherle.truezip.key.pbe.swing.InvalidKeyFeedback;
import de.schlichtherle.truezip.key.pbe.swing.HurlingWindowFeedback;
import java.io.ByteArrayOutputStream;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public class ApplicationTest {

    Application instance;

    @Before
    public void setUp() {
        instance = new TestApplication();
    }

    @Test
    public void testSetup() {
        instance.setup();
        assertEquals(HurlingWindowFeedback.class.getName(),
                System.getProperty(InvalidKeyFeedback.class.getName()));
    }

    @Test
    public void testWork() {
        assertEquals(1, instance.work(null));
        assertEquals(0, instance.work(new String[0]));
    }

    private static final class TestApplication extends Application {
        TestApplication() {
            super(new ByteArrayOutputStream(), new ByteArrayOutputStream(), false);
        }

        @Override
        public int runChecked(String[] args) throws Exception {
            if (null == args)
                throw new NullPointerException();
            return 0;
        }
    } // TestApplication
}
