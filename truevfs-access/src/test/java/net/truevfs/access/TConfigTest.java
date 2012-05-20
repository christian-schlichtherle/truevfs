/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.access;

import net.truevfs.access.TFile;
import net.truevfs.access.TArchiveDetector;
import net.truevfs.access.TConfig;
import static net.truevfs.kernel.FsAccessOption.*;
import net.truevfs.kernel.FsDriver;
import net.truevfs.kernel.mock.MockArchiveDriver;
import net.truevfs.kernel.util.BitField;
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
public final class TConfigTest {

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
        assertThat(c.getAccessPreferences(), is(BitField.of(CREATE_PARENTS)));
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
        try (final TConfig c1 = TConfig.push()) {
            try {
                try (final TConfig c2 = TConfig.push()) {
                    try {
                        c1.close();
                        fail();
                    } catch (IllegalStateException notTopElement) {
                        assertSame(c2, TConfig.get());
                    } finally {
                        c2.close();
                    }
                }
                assertSame(c1, TConfig.get());
            } finally {
                c1.close();
            }
        }
    }

    @Test
    public void inheritance() throws InterruptedException {
        assertInheritance();
        try (final TConfig c1 = TConfig.push()) {
            assertInheritance();
            try (final TConfig c2 = TConfig.push()) {
                assertInheritance();
            }
            assertInheritance();
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
        try (final TConfig c = TConfig.push()) {
            assertTrue(c.isLenient());
            assertThat(c.getAccessPreferences(), is(BitField.of(CREATE_PARENTS)));

            c.setLenient(false);

            assertFalse(c.isLenient());
            assertTrue(c.getAccessPreferences().isEmpty());

            c.setLenient(true);

            assertTrue(c.isLenient());
            assertThat(c.getAccessPreferences(), is(BitField.of(CREATE_PARENTS)));

            c.setAccessPreferences(BitField.of(CACHE));

            assertFalse(c.isLenient());
            assertThat(c.getAccessPreferences(), is(BitField.of(CACHE)));

            c.setAccessPreferences(BitField.of(CREATE_PARENTS));

            assertTrue(c.isLenient());
            assertThat(c.getAccessPreferences(), is(BitField.of(CREATE_PARENTS)));

            try {
                c.setAccessPreferences(BitField.of(APPEND));
                fail();
            } catch (IllegalArgumentException expected) {
            }

            assertTrue(c.isLenient());
            assertThat(c.getAccessPreferences(), is(BitField.of(CREATE_PARENTS)));

            try {
                c.setAccessPreferences(BitField.of(EXCLUSIVE));
                fail();
            } catch (IllegalArgumentException expected) {
            }

            assertTrue(c.isLenient());
            assertThat(c.getAccessPreferences(), is(BitField.of(CREATE_PARENTS)));

            c.setAccessPreferences(BitField.of(STORE));

            assertFalse(c.isLenient());
            assertThat(c.getAccessPreferences(), is(BitField.of(STORE)));

            c.setAccessPreferences(BitField.of(COMPRESS));

            assertFalse(c.isLenient());
            assertThat(c.getAccessPreferences(), is(BitField.of(COMPRESS)));

            c.setAccessPreferences(BitField.of(GROW));

            assertFalse(c.isLenient());
            assertThat(c.getAccessPreferences(), is(BitField.of(GROW)));

            c.setAccessPreferences(BitField.of(CACHE, CREATE_PARENTS, COMPRESS));

            assertTrue(c.isLenient());
            assertThat(c.getAccessPreferences(), is(BitField.of(CACHE, CREATE_PARENTS, COMPRESS)));

            c.setAccessPreferences(BitField.of(CREATE_PARENTS));

            try {
                c.setAccessPreferences(BitField.of(STORE, COMPRESS));
                fail();
            } catch (IllegalArgumentException expected) {
            }

            assertTrue(c.isLenient());
            assertThat(c.getAccessPreferences(), is(BitField.of(CREATE_PARENTS)));
        }
    }

    @Test
    public void standardUseCase() {
        TFile f1 = new TFile("file.mok");
        assertFalse(f1.isArchive());
        try (final TConfig c = TConfig.push()) {
            // Change the inheritable thread local configuration.
            c.setArchiveDetector(
                    new TArchiveDetector("mok", new MockArchiveDriver()));
            // Use the inheritable thread local configuration.
            TFile f2 = new TFile("file.mok");
            assertTrue(f2.isArchive());
            // Do some I/O here.
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
        try (final TConfig c2 = TConfig.push()) {
            c2.setArchiveDetector(ad2);
            assertThat(TConfig.get(), sameInstance(c2));
            assertThat(c2.getArchiveDetector(), sameInstance(ad2));
            final TArchiveDetector ad3 = new TArchiveDetector("mok", d);
            try (final TConfig c3 = TConfig.push()) {
                c3.setArchiveDetector(ad3);
                assertThat(TConfig.get(), sameInstance(c3));
                assertThat(c3.getArchiveDetector(), sameInstance(ad3));
            }
            assertThat(TConfig.get(), sameInstance(c2));
            assertThat(c2.getArchiveDetector(), sameInstance(ad2));
        }
        assertThat(TConfig.get(), sameInstance(c1));
        assertThat(c1.getArchiveDetector(), sameInstance(ad1));
    }
}