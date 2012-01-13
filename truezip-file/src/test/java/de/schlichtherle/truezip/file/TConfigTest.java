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
import static de.schlichtherle.truezip.fs.FsInputOptions.NO_INPUT_OPTIONS;
import static de.schlichtherle.truezip.fs.FsOutputOption.*;
import de.schlichtherle.truezip.fs.archive.mock.MockArchiveDriver;
import de.schlichtherle.truezip.util.BitField;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.NoSuchElementException;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
public class TConfigTest {

    @Test
    public void defaults() {
        final TConfig c = TConfig.get();
        assertThat(TConfig.get(), sameInstance(c));
        final TArchiveDetector detector = c.getArchiveDetector();
        assertThat(detector, sameInstance(TArchiveDetector.ALL));
        assertThat(c.getArchiveDetector(), sameInstance(detector));
        final boolean lenient = c.isLenient();
        assertThat(lenient, is(true));
        assertThat(c.isLenient(), is(lenient));
        assertTrue(c.getInputPreferences().isEmpty());
        assertThat(c.getOutputPreferences(), is(BitField.of(CREATE_PARENTS)));
    }

    @Test
    public void pop() {
        try {
            TConfig.pop();
            fail();
        } catch (IllegalStateException emptyStack) {
            assertTrue(emptyStack.getCause() instanceof NoSuchElementException);
        }
    }

    @Test
    public void close() {
        final TConfig c1 = TConfig.push();
        try {
            final TConfig c2 = TConfig.push();
            try {
                c1.close();
                fail();
            } catch (IllegalStateException notTopElement) {
            } finally {
                c2.close();
                try {
                    c2.close();
                    fail();
                } catch (IllegalStateException alreadyClosed) {
                }
            }
        } finally {
            c1.close();
            try {
                c1.close();
                fail();
            } catch (IllegalStateException emptyStack) {
                assertTrue(emptyStack.getCause() instanceof NoSuchElementException);
            }
        }
    }

    @Test
    public void inheritance() throws InterruptedException {
        assertInheritance();
        final TConfig c1 = TConfig.push();
        try {
            assertInheritance();
            final TConfig c2 = TConfig.push();
            try {
                assertInheritance();
            } finally {
                c2.close();
            }
            assertInheritance();
        } finally {
            c1.close();
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
                    TConfig.pop(); // must fail!
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
    public void preferences() {
        final TConfig c = TConfig.push();
        try {
            assertTrue(c.isLenient());
            assertTrue(c.getInputPreferences().isEmpty());
            assertThat(c.getOutputPreferences(), is(BitField.of(CREATE_PARENTS)));

            c.setLenient(false);

            assertFalse(c.isLenient());
            assertTrue(c.getInputPreferences().isEmpty());
            assertTrue(c.getOutputPreferences().isEmpty());

            c.setLenient(true);

            assertTrue(c.isLenient());
            assertTrue(c.getInputPreferences().isEmpty());
            assertThat(c.getOutputPreferences(), is(BitField.of(CREATE_PARENTS)));

            c.setInputPreferences(BitField.of(FsInputOption.CACHE));

            assertTrue(c.isLenient());
            assertThat(c.getInputPreferences(), is(BitField.of(FsInputOption.CACHE)));
            assertThat(c.getOutputPreferences(), is(BitField.of(CREATE_PARENTS)));

            c.setInputPreferences(NO_INPUT_OPTIONS);

            assertTrue(c.isLenient());
            assertTrue(c.getInputPreferences().isEmpty());
            assertThat(c.getOutputPreferences(), is(BitField.of(CREATE_PARENTS)));

            c.setOutputPreferences(BitField.of(CACHE));

            assertFalse(c.isLenient());
            assertTrue(c.getInputPreferences().isEmpty());
            assertThat(c.getOutputPreferences(), is(BitField.of(CACHE)));

            c.setOutputPreferences(BitField.of(CREATE_PARENTS));

            assertTrue(c.isLenient());
            assertTrue(c.getInputPreferences().isEmpty());
            assertThat(c.getOutputPreferences(), is(BitField.of(CREATE_PARENTS)));

            try {
                c.setOutputPreferences(BitField.of(APPEND));
                fail();
            } catch (IllegalArgumentException expected) {
            }

            assertTrue(c.isLenient());
            assertTrue(c.getInputPreferences().isEmpty());
            assertThat(c.getOutputPreferences(), is(BitField.of(CREATE_PARENTS)));

            try {
                c.setOutputPreferences(BitField.of(EXCLUSIVE));
                fail();
            } catch (IllegalArgumentException expected) {
            }

            assertTrue(c.isLenient());
            assertTrue(c.getInputPreferences().isEmpty());
            assertThat(c.getOutputPreferences(), is(BitField.of(CREATE_PARENTS)));

            c.setOutputPreferences(BitField.of(STORE));

            assertFalse(c.isLenient());
            assertTrue(c.getInputPreferences().isEmpty());
            assertThat(c.getOutputPreferences(), is(BitField.of(STORE)));

            c.setOutputPreferences(BitField.of(COMPRESS));

            assertFalse(c.isLenient());
            assertTrue(c.getInputPreferences().isEmpty());
            assertThat(c.getOutputPreferences(), is(BitField.of(COMPRESS)));

            c.setOutputPreferences(BitField.of(GROW));

            assertFalse(c.isLenient());
            assertTrue(c.getInputPreferences().isEmpty());
            assertThat(c.getOutputPreferences(), is(BitField.of(GROW)));

            c.setOutputPreferences(BitField.of(CACHE, CREATE_PARENTS, COMPRESS));

            assertTrue(c.isLenient());
            assertTrue(c.getInputPreferences().isEmpty());
            assertThat(c.getOutputPreferences(), is(BitField.of(CACHE, CREATE_PARENTS, COMPRESS)));

            c.setOutputPreferences(BitField.of(CREATE_PARENTS));

            try {
                c.setOutputPreferences(BitField.of(STORE, COMPRESS));
                fail();
            } catch (IllegalArgumentException expected) {
            }

            assertTrue(c.isLenient());
            assertTrue(c.getInputPreferences().isEmpty());
            assertThat(c.getOutputPreferences(), is(BitField.of(CREATE_PARENTS)));
        } finally {
            c.close();
        }
    }

    @Test
    public void standardUseCase() {
        TFile f1 = new TFile("file.mok");
        assertFalse(f1.isArchive());
        // Push a new current configuration on the thread local stack.
        TConfig c = TConfig.push();
        try {
            // Change the inheritable thread local configuration.
            c.setArchiveDetector(new TArchiveDetector("mok", new MockArchiveDriver()));
            // Use the inheritable thread local configuration.
            TFile f2 = new TFile("file.mok");
            assertTrue(f2.isArchive());
            // Do some I/O here.
        } finally {
            // Pop the configuration off the inheritable thread local stack.
            c.close();
        }
    }

    @Test
    public void advancedUseCase() {
        final TConfig c1 = TConfig.get();
        assertThat(TConfig.get(), sameInstance(c1));
        final TArchiveDetector detector1 = c1.getArchiveDetector();
        assertThat(c1.getArchiveDetector(), sameInstance(detector1));
        final TArchiveDetector detector2 = new TArchiveDetector("mok", new MockArchiveDriver());
        final TConfig c2 = TConfig.push();
        try {
            c2.setArchiveDetector(detector2);
            assertThat(TConfig.get(), sameInstance(c2));
            assertThat(c2.getArchiveDetector(), sameInstance(detector2));
            final TArchiveDetector detector3 = new TArchiveDetector("mok", new MockArchiveDriver());
            final TConfig config3 = TConfig.push();
            try {
                config3.setArchiveDetector(detector3);
                assertThat(TConfig.get(), sameInstance(config3));
                assertThat(config3.getArchiveDetector(), sameInstance(detector3));
            } finally {
                config3.close();
            }
            assertThat(TConfig.get(), sameInstance(c2));
            assertThat(c2.getArchiveDetector(), sameInstance(detector2));
        } finally {
            c2.close();
        }
        assertThat(TConfig.get(), sameInstance(c1));
        assertThat(c1.getArchiveDetector(), sameInstance(detector1));
    }
}
