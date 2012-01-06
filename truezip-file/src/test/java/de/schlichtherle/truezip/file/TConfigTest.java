/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.file;

import de.schlichtherle.truezip.fs.FsInputOption;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import de.schlichtherle.truezip.fs.archive.mock.MockArchiveDriver;
import static de.schlichtherle.truezip.fs.FsInputOptions.*;
import static de.schlichtherle.truezip.fs.FsOutputOption.*;
import de.schlichtherle.truezip.util.BitField;
import static org.junit.Assert.*;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.*;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
public class TConfigTest {

    @Test
    public void testDefaults() {
        final TConfig config = TConfig.get();
        assertThat(TConfig.get(), sameInstance(config));
        final TArchiveDetector detector = config.getArchiveDetector();
        assertThat(detector, sameInstance(TArchiveDetector.ALL));
        assertThat(config.getArchiveDetector(), sameInstance(detector));
        final boolean lenient = config.isLenient();
        assertThat(lenient, is(true));
        assertThat(config.isLenient(), is(lenient));
        assertTrue(config.getInputPreferences().isEmpty());
        assertThat(config.getOutputPreferences(), is(BitField.of(CREATE_PARENTS)));
    }

    @Test
    public void testPop() {
        try {
            TConfig.pop();
            fail();
        } catch (IllegalStateException expected) {
        }
    }

    @Test
    public void testInheritance() throws InterruptedException {
        assertInheritance();
        final TConfig config1 = TConfig.push();
        try {
            assertInheritance();
            final TConfig config2 = TConfig.push();
            try {
                assertInheritance();
            } finally {
                config2.close();
            }
            assertInheritance();
        } finally {
            config1.close();
        }
    }

    private void assertInheritance() throws InterruptedException {
        class TestThread extends Thread {
            TConfig config;

            @Override
            public void run() {
                config = TConfig.get();
                try {
                    TConfig.pop(); // may fail if the current configuration is the global configuration
                    TConfig.pop(); // must fail
                    fail();
                } catch (IllegalStateException expected) {
                }
            }
        } // class TestThread

        TestThread t = new TestThread();
        t.start();
        t.join();
        assertThat(t.config, sameInstance(TConfig.get()));
    }

    @Test
    public void testPreferences() {
        final TConfig config = TConfig.push();
        try {
            assertTrue(config.isLenient());
            assertTrue(config.getInputPreferences().isEmpty());
            assertThat(config.getOutputPreferences(), is(BitField.of(CREATE_PARENTS)));

            config.setLenient(false);

            assertFalse(config.isLenient());
            assertTrue(config.getInputPreferences().isEmpty());
            assertTrue(config.getOutputPreferences().isEmpty());

            config.setLenient(true);

            assertTrue(config.isLenient());
            assertTrue(config.getInputPreferences().isEmpty());
            assertThat(config.getOutputPreferences(), is(BitField.of(CREATE_PARENTS)));

            config.setInputPreferences(BitField.of(FsInputOption.CACHE));

            assertTrue(config.isLenient());
            assertThat(config.getInputPreferences(), is(BitField.of(FsInputOption.CACHE)));
            assertThat(config.getOutputPreferences(), is(BitField.of(CREATE_PARENTS)));

            config.setInputPreferences(NO_INPUT_OPTIONS);

            assertTrue(config.isLenient());
            assertTrue(config.getInputPreferences().isEmpty());
            assertThat(config.getOutputPreferences(), is(BitField.of(CREATE_PARENTS)));

            config.setOutputPreferences(BitField.of(CACHE));

            assertFalse(config.isLenient());
            assertTrue(config.getInputPreferences().isEmpty());
            assertThat(config.getOutputPreferences(), is(BitField.of(CACHE)));

            config.setOutputPreferences(BitField.of(CREATE_PARENTS));

            assertTrue(config.isLenient());
            assertTrue(config.getInputPreferences().isEmpty());
            assertThat(config.getOutputPreferences(), is(BitField.of(CREATE_PARENTS)));

            try {
                config.setOutputPreferences(BitField.of(APPEND));
                fail();
            } catch (IllegalArgumentException expected) {
            }

            assertTrue(config.isLenient());
            assertTrue(config.getInputPreferences().isEmpty());
            assertThat(config.getOutputPreferences(), is(BitField.of(CREATE_PARENTS)));

            try {
                config.setOutputPreferences(BitField.of(EXCLUSIVE));
                fail();
            } catch (IllegalArgumentException expected) {
            }

            assertTrue(config.isLenient());
            assertTrue(config.getInputPreferences().isEmpty());
            assertThat(config.getOutputPreferences(), is(BitField.of(CREATE_PARENTS)));

            config.setOutputPreferences(BitField.of(STORE));

            assertFalse(config.isLenient());
            assertTrue(config.getInputPreferences().isEmpty());
            assertThat(config.getOutputPreferences(), is(BitField.of(STORE)));

            config.setOutputPreferences(BitField.of(COMPRESS));

            assertFalse(config.isLenient());
            assertTrue(config.getInputPreferences().isEmpty());
            assertThat(config.getOutputPreferences(), is(BitField.of(COMPRESS)));

            config.setOutputPreferences(BitField.of(GROW));

            assertFalse(config.isLenient());
            assertTrue(config.getInputPreferences().isEmpty());
            assertThat(config.getOutputPreferences(), is(BitField.of(GROW)));

            config.setOutputPreferences(BitField.of(CACHE, CREATE_PARENTS, COMPRESS));

            assertTrue(config.isLenient());
            assertTrue(config.getInputPreferences().isEmpty());
            assertThat(config.getOutputPreferences(), is(BitField.of(CACHE, CREATE_PARENTS, COMPRESS)));

            config.setOutputPreferences(BitField.of(CREATE_PARENTS));

            try {
                config.setOutputPreferences(BitField.of(STORE, COMPRESS));
                fail();
            } catch (IllegalArgumentException expected) {
            }

            assertTrue(config.isLenient());
            assertTrue(config.getInputPreferences().isEmpty());
            assertThat(config.getOutputPreferences(), is(BitField.of(CREATE_PARENTS)));
        } finally {
            config.close();
        }
    }

    @Test
    public void runStandardUseCase() {
        TFile file1 = new TFile("file.mok");
        assertFalse(file1.isArchive());
        // Push a new current configuration on the thread local stack.
        TConfig config = TConfig.push();
        try {
            // Change the inheritable thread local configuration.
            config.setArchiveDetector(new TArchiveDetector("mok", new MockArchiveDriver()));
            // Use the inheritable thread local configuration.
            TFile file2 = new TFile("file.mok");
            assertTrue(file2.isArchive());
            // Do some I/O here.
        } finally {
            // Pop the configuration off the inheritable thread local stack.
            config.close();
        }
    }

    @Test
    public void testAdvancedUseCase() {
        final TConfig config1 = TConfig.get();
        assertThat(TConfig.get(), sameInstance(config1));
        final TArchiveDetector detector1 = config1.getArchiveDetector();
        assertThat(config1.getArchiveDetector(), sameInstance(detector1));
        final TArchiveDetector detector2 = new TArchiveDetector("mok", new MockArchiveDriver());
        final TConfig config2 = TConfig.push();
        try {
            config2.setArchiveDetector(detector2);
            assertThat(TConfig.get(), sameInstance(config2));
            assertThat(config2.getArchiveDetector(), sameInstance(detector2));
            final TArchiveDetector detector3 = new TArchiveDetector("mok", new MockArchiveDriver());
            final TConfig config3 = TConfig.push();
            try {
                config3.setArchiveDetector(detector3);
                assertThat(TConfig.get(), sameInstance(config3));
                assertThat(config3.getArchiveDetector(), sameInstance(detector3));
            } finally {
                config3.close();
            }
            assertThat(TConfig.get(), sameInstance(config2));
            assertThat(config2.getArchiveDetector(), sameInstance(detector2));
        } finally {
            config2.close();
        }
        assertThat(TConfig.get(), sameInstance(config1));
        assertThat(config1.getArchiveDetector(), sameInstance(detector1));
    }
}
