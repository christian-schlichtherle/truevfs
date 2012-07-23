/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.samples.access;

import java.io.ByteArrayOutputStream;
import java.util.Objects;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * @author Christian Schlichtherle
 */
public class ApplicationTest {

    final Application instance = new TestApplication();

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
