/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.file;

import net.truevfs.kernel.FsArchiveDriver;
import net.truevfs.kernel.FsDriver;
import net.truevfs.kernel.FsScheme;
import net.truevfs.kernel.mock.MockArchiveDriver;
import net.truevfs.kernel.util.ExtensionSet;
import java.io.File;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nullable;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Christian Schlichtherle
 */
public final class TArchiveDetectorTest {

    private FsArchiveDriver<?> driver;
    private TArchiveDetector ALL, NIL, MOK;

    @Before
    public void setUp() {
        driver = new MockArchiveDriver();
        ALL = new TArchiveDetector("tar.gz|zip", driver);
        NIL = new TArchiveDetector(ALL, ""); // test decoration
        MOK = new TArchiveDetector(NIL, "nil", driver); // test decoration
    }

    @Test
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testIllegalConstructors() throws Throwable {
        for (TArchiveDetector delegate : new TArchiveDetector[] {
            NIL,
            ALL,
        }) {
            try {
                new TArchiveDetector(delegate, new Object[][] {
                    { "foo", "java.lang.Object", },
                    { "bar", "java.io.FilterInputStream", },
                });
                fail("Expected IllegalArgumentException");
            } catch (IllegalArgumentException expected) {
            }
        }

        assertIllegalConstructors(NullPointerException.class,
                new Object[][] {
                    { null, null },
                    { null, driver },
                    //{ "xyz", null },
                    { null, null, null },
                    { null, null, driver },
                    { null, "xyz", null },
                    { null, "xyz", driver },
                    { NIL, null, null },
                    { NIL, null, driver },
                    //{ TArchiveDetector.NULL, "xyz", null },
                    { null, new Object[][] {{ "xyz", MockArchiveDriver.class }} },
                    { NIL, null },
                    { NIL, new Object[][] {{ null, null }} },
                    { NIL, new Object[][] {{ null, "" }} },
                    { NIL, new Object[][] {{ null, "xyz" }} },
                    //{ TArchiveDetector.NULL, new Object[] { "xyz", null } },
               });

        assertIllegalConstructors(IllegalArgumentException.class,
                new Object[][] {
                    { "DRIVER" },
                    { "DEFAULT" },
                    { "NULL" },
                    { "ALL" },
                    { "unknownExtension" },
                    { "", driver }, // empty extension set
                    { ".", driver }, // empty extension set
                    { "|", driver }, // empty extension set
                    { "|.", driver }, // empty extension set
                    { "||", driver }, // empty extension set
                    { "||.", driver }, // empty extension set
                    { "|.|", driver }, // empty extension set
                    { "|.|.", driver }, // empty extension set
                    { NIL, "", driver }, // empty extension set
                    { NIL, ".", driver }, // empty extension set
                    { NIL, "|", driver }, // empty extension set
                    { NIL, "|.", driver }, // empty extension set
                    { NIL, "||", driver }, // empty extension set
                    { NIL, "||.", driver }, // empty extension set
                    { NIL, "|.|", driver }, // empty extension set
                    { NIL, "|.|.", driver }, // empty extension set
                    { NIL, new Object[][] {{ "", driver }} }, // empty extension set
                    { NIL, new Object[][] {{ ".", driver }} }, // empty extension set
                    { NIL, new Object[][] {{ "|", driver }} }, // empty extension set
                    { NIL, new Object[][] {{ "|.", driver }} }, // empty extension set
                    { NIL, new Object[][] {{ "||", driver }} }, // empty extension set
                    { NIL, new Object[][] {{ "||.", driver }} }, // empty extension set
                    { NIL, new Object[][] {{ "|.|", driver }} }, // empty extension set
                    { NIL, new Object[][] {{ "|.|.", driver }} }, // empty extension set
                    { NIL, new Object[][] {{ "anyExtension", "" }} }, // empty class name
                    { NIL, new Object[][] {{ "anyExtension", "xyz" }} }, // not a class name
                    { NIL, new Object[][] {{ MockArchiveDriver.class, driver }} }, // not a extension list
                    { NIL, new Object[][] {{ driver, driver }} }, // not a extension list
                    { NIL, new Object[][] {{ "anyExtension", new Object() }} }, // not an archive driver
                    { NIL, new Object[][] {{ "anyExtension", Object.class }} }, // not an archive driver class
                });
    }

    @SuppressWarnings({ "unchecked", "ResultOfObjectAllocationIgnored" })
    private void assertIllegalConstructors(
            final Class<? extends Throwable> expected,
            final Object[][] list)
    throws Throwable {
        for (int i = 0; i < list.length; i++) {
            final Object[] args = list[i];
            Object arg0 = args[0], arg1, arg2;
            try {
                switch (args.length) {
                    case 1:
                        new TArchiveDetector((String) arg0);
                        fail("Index " + i);
                        break;

                    case 2:
                        arg1 = args[1];
                        if (arg0 != null) {
                            if (arg1 != null) {
                                if (arg0 instanceof String)
                                    new TArchiveDetector((String) arg0, (FsArchiveDriver<?>) arg1);
                                else if (arg1 instanceof Object[][])
                                    new TArchiveDetector((TArchiveDetector) arg0, (Object[][]) arg1);
                                else
                                    new TArchiveDetector((TArchiveDetector) arg0, (Map<FsScheme, FsDriver>) arg1);
                                fail("Index " + i);
                            } else {
                                assert arg0 != null;
                                assert arg1 == null;
                                if (arg0 instanceof String) {
                                    new TArchiveDetector((String) arg0, null);
                                    fail("Index " + i);
                                } else {
                                    try {
                                        new TArchiveDetector((TArchiveDetector) arg0, (Object[][]) null);
                                        fail("Index " + i);
                                    } catch (Throwable failure) {
                                        assertTrue(expected.isAssignableFrom(failure.getClass()));
                                    }
                                    try {
                                        new TArchiveDetector((TArchiveDetector) arg0, (Map<FsScheme, FsDriver>) null);
                                        fail("Index " + i);
                                    } catch (Throwable failure) {
                                        assertTrue(expected.isAssignableFrom(failure.getClass()));
                                    }
                                }
                            }
                        } else {
                            assert arg0 == null;
                            if (arg1 != null) {
                                if (arg1 instanceof FsArchiveDriver<?>)
                                    new TArchiveDetector(null, (FsArchiveDriver<?>) arg1);
                                else if (arg1 instanceof Object[][])
                                    new TArchiveDetector(null, (Object[][]) arg1);
                                else
                                    new TArchiveDetector(null, (Map<FsScheme, FsDriver>) arg1);
                                fail("Index " + i);
                            } else {
                                assert arg0 == null;
                                assert arg1 == null;
                                try {
                                    new TArchiveDetector((String) null, (FsArchiveDriver<?>) null);
                                    fail("Index " + i);
                                } catch (Throwable failure) {
                                    assertTrue(expected.isAssignableFrom(failure.getClass()));
                                }
                                try {
                                    new TArchiveDetector((TArchiveDetector) null, (Object[][]) null);
                                    fail("Index " + i);
                                } catch (Throwable failure) {
                                    assertTrue(expected.isAssignableFrom(failure.getClass()));
                                }
                                try {
                                    new TArchiveDetector((TArchiveDetector) null, (Map<FsScheme, FsDriver>) null);
                                    fail("Index " + i);
                                } catch (Throwable failure) {
                                    assertTrue(expected.isAssignableFrom(failure.getClass()));
                                }
                            }
                        }
                        break;

                    case 3:
                        arg1 = args[1];
                        arg2 = args[2];
                        new TArchiveDetector((TArchiveDetector) arg0, (String) arg1, (FsArchiveDriver<?>) arg2);
                        fail("Index " + i);
                        break;

                    default:
                        throw new AssertionError();
                }
            } catch (final Throwable ex) {
                if (!expected.isAssignableFrom(ex.getClass()))
                    throw ex;
            }
        }
    }

    @Test
    public void testGetExtensions() {
        assertExtensions(new String[] {
            "zip", "zip",
            "zip", ".zip",
            "zip", "|zip",
            "zip", "zip|",
            "zip", "zip|zip",
            "zip", "zip|.zip",
            "zip", "zip||zip",
            "zip", "zip|zip|",
            "zip", ".zip|",
            "zip", ".zip|zip",
            "zip", ".zip|.zip",
            "zip", ".zip||zip",
            "zip", ".zip|zip|",
            "zip", "|zip|",
            "zip", "|zip|zip",
            "zip", "|zip|.zip",
            "zip", "|zip||zip",
            "zip", "|zip|zip|",

            "zip", "ZIP",
            "zip", ".ZIP",
            "zip", "|ZIP",
            "zip", "ZIP|",
            "zip", "ZIP|ZIP",
            "zip", "ZIP|.ZIP",
            "zip", "ZIP||ZIP",
            "zip", "ZIP|ZIP|",
            "zip", ".ZIP|",
            "zip", ".ZIP|ZIP",
            "zip", ".ZIP|.ZIP",
            "zip", ".ZIP||ZIP",
            "zip", ".ZIP|ZIP|",
            "zip", "|ZIP|",
            "zip", "|ZIP|ZIP",
            "zip", "|ZIP|.ZIP",
            "zip", "|ZIP||ZIP",
            "zip", "|ZIP|ZIP|",

            "tar.gz|zip", "TAR.GZ|ZIP",
            "tar.gz|zip", "ZIP|TAR.GZ",
            "tar.gz|zip", "|ZIP|TAR.GZ",
            "tar.gz|zip", "ZIP|TAR.GZ|",
            "tar.gz|zip", "|ZIP|TAR.GZ|",
            "tar.gz|zip", "||ZIP|TAR.GZ|",
            "tar.gz|zip", "|ZIP||TAR.GZ|",
            "tar.gz|zip", "|ZIP|TAR.GZ||",

            "tar.gz|zip", ".TAR.GZ|.ZIP",
            "tar.gz|zip", ".ZIP|.TAR.GZ",
            "tar.gz|zip", "|.ZIP|.TAR.GZ",
            "tar.gz|zip", ".ZIP|.TAR.GZ|",
            "tar.gz|zip", "|.ZIP|.TAR.GZ|",
            "tar.gz|zip", "||.ZIP|.TAR.GZ|",
            "tar.gz|zip", "|.ZIP||.TAR.GZ|",
            "tar.gz|zip", "|.ZIP|.TAR.GZ||",
        });
    }

    private void assertExtensions(final String[] args) {
        for (int i = 0; i < args.length; ) {
            final String result = args[i++];
            final String extensions = args[i++];
            TArchiveDetector
            detector = new TArchiveDetector(extensions, driver);
            assertEquals(result, detector.toString());
            detector = new TArchiveDetector(NIL, extensions, driver);
            assertEquals(result, detector.toString());
            detector = new TArchiveDetector(NIL, new Object[][] {{ extensions, driver }});
            assertEquals(result, detector.toString());
        }
    }

    @Test
    public void testNullMapping() {
        for (TArchiveDetector delegate : new TArchiveDetector[] {
            NIL,
            ALL,
        }) {
            TArchiveDetector detector = new TArchiveDetector(
                    delegate, "zip", null); // remove zip extension
            assertFalse(new ExtensionSet(detector.toString()).contains("zip"));
            detector = new TArchiveDetector(
                    delegate, ".ZIP", null); // remove zip extension
            assertFalse(new ExtensionSet(detector.toString()).contains("zip"));
        }
    }

    @Test
    public void testGetDriver() {
        assertScheme(new String[][] {
            { null, "" },
            { null, "." },
            { null, ".all" },
            { null, ".default" },
            { null, ".ear" },
            { null, ".exe" },
            { null, ".file" },
            { null, ".null" },
            { null, ".z" },
            { null, "test" },
            { null, "test." },
            { null, "test.all" },
            { null, "test.default" },
            { null, "test.null" },
            { null, "test.z" },
        }, NIL, MOK, ALL);

        assertScheme(new String[][] {
            { null, ".tar.gz" },
            { null, ".zip" },
            { null, "test.tar.gz" },
            { null, "test.zip" },
        }, NIL, MOK);

        assertScheme(new String[][] {
            { "tar.gz", ".tar.gz" },
            { "tar.gz", "test.tar.gz" },
            { "tar.gz", "foo" + File.separator + "test.123.tar.gz" },
            { "zip", ".zip" },
            { "zip", "test.zip" },
            { "zip", "foo" + File.separator + "test.123.zip" },
        }, ALL);
    }

    @SuppressWarnings("AssignmentToForLoopParameter")
    private void assertScheme(
            final String[][] tests,
            final TArchiveDetector... detectors) {
        for (TArchiveDetector detector : detectors) {
            try {
                detector.scheme(null);
                fail("Expected NullPointerException!");
            } catch (NullPointerException expected) {
            }

            for (String[] test : tests) {
                final FsScheme scheme = test[0] == null ? null : FsScheme.create(test[0]);
                final String path = test[1];
                assertScheme(detector, scheme, path);

                // Add level of indirection in order to test caching.
                detector = new TArchiveDetector(detector, new Object[0][0]);
                assertScheme(detector, scheme, path);
            }
        }
    }

    private void assertScheme(
            final TArchiveDetector detector,
            final @Nullable FsScheme scheme,
            final String path) {
        final String lpath = path.toLowerCase(Locale.ENGLISH);
        final String upath = path.toUpperCase(Locale.ENGLISH);

        assertThat(detector.scheme(lpath), equalTo(scheme));
        assertThat(detector.scheme(upath), equalTo(scheme));
    }
}
