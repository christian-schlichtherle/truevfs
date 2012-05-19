/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.samples.file;

import de.schlichtherle.truevfs.key.pbe.swing.feedback.HurlingWindowFeedback;
import de.schlichtherle.truevfs.key.pbe.swing.feedback.InvalidKeyFeedback;
import java.io.ByteArrayOutputStream;
import java.util.Objects;
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
            Objects.requireNonNull(args);
            return 0;
        }
    } // TestApplication
}