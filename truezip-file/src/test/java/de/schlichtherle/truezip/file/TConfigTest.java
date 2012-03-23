/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.file;

import de.schlichtherle.truezip.fs.FsDriver;
import de.schlichtherle.truezip.fs.FsInputOption;
import de.schlichtherle.truezip.fs.FsInputOptions;
import static de.schlichtherle.truezip.fs.FsOutputOption.*;
import de.schlichtherle.truezip.fs.mock.MockArchiveDriver;
import de.schlichtherle.truezip.util.BitField;
import java.util.NoSuchElementException;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * DO NOT MODIFY THE GLOBAL CONFIGURATION IN THESE TESTS!
 * Its global scope makes it available to any other test running in parallel,
 * if any.
 * 
 * @author  Christian Schlichtherle
 */
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
        TConfig.push();
        TConfig.pop();
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
                assertSame(c2, TConfig.get());
            } finally {
                c2.close();
            }
            c2.close(); // should get ignored
            assertSame(c1, TConfig.get());
        } finally {
            c1.close();
        }
        c1.close(); // should get ignored
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
        } // TestThread

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

            c.setInputPreferences(FsInputOptions.NONE);

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
            c.setArchiveDetector(
                    new TArchiveDetector("mok", new MockArchiveDriver()));
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
        final FsDriver d = new MockArchiveDriver();
        final TConfig c1 = TConfig.get();
        assertThat(TConfig.get(), sameInstance(c1));
        final TArchiveDetector ad1 = c1.getArchiveDetector();
        assertThat(c1.getArchiveDetector(), sameInstance(ad1));
        final TArchiveDetector ad2 = new TArchiveDetector("mok", d);
        final TConfig c2 = TConfig.push();
        try {
            c2.setArchiveDetector(ad2);
            assertThat(TConfig.get(), sameInstance(c2));
            assertThat(c2.getArchiveDetector(), sameInstance(ad2));
            final TArchiveDetector ad3 = new TArchiveDetector("mok", d);
            final TConfig c3 = TConfig.push();
            try {
                c3.setArchiveDetector(ad3);
                assertThat(TConfig.get(), sameInstance(c3));
                assertThat(c3.getArchiveDetector(), sameInstance(ad3));
            } finally {
                c3.close();
            }
            assertThat(TConfig.get(), sameInstance(c2));
            assertThat(c2.getArchiveDetector(), sameInstance(ad2));
        } finally {
            c2.close();
        }
        assertThat(TConfig.get(), sameInstance(c1));
        assertThat(c1.getArchiveDetector(), sameInstance(ad1));
    }
}