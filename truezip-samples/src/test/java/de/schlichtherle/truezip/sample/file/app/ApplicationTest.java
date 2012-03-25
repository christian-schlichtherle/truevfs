/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.sample.file.app;

import de.truezip.kernel.key.pbe.swing.HurlingWindowFeedback;
import de.truezip.kernel.key.pbe.swing.InvalidKeyFeedback;
import java.io.ByteArrayOutputStream;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

/**
 * @author  Christian Schlichtherle
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
        protected int runChecked(String[] args) throws Exception {
            if (null == args)
                throw new NullPointerException();
            return 0;
        }
    } // TestApplication
}