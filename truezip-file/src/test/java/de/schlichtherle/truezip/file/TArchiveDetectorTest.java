/*
 * Copyright (C) 2005-2011 Schlichtherle IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.schlichtherle.truezip.file;

import de.schlichtherle.truezip.fs.FsDriver;
import de.schlichtherle.truezip.fs.FsScheme;
import de.schlichtherle.truezip.fs.archive.mock.MockArchiveDriver;
import de.schlichtherle.truezip.fs.archive.FsArchiveDriver;
import de.schlichtherle.truezip.util.SuffixSet;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Locale;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
public class TArchiveDetectorTest {

    private FsArchiveDriver<?> driver;
    private TArchiveDetector ALL, NULL;

    @Before
    public void setUp() {
        driver = new MockArchiveDriver();
        ALL = new TArchiveDetector("jar|zip", driver);
        NULL = new TArchiveDetector(ALL, ""); // test decoration
    }

    @Test
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testIllegalConstructors() {
        for (TArchiveDetector delegate : new TArchiveDetector[] {
            NULL,
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

        testIllegalConstructors(NullPointerException.class,
                new Object[][] {
                    { null, null },
                    { null, driver },
                    //{ "xyz", null },
                    { null, null, null },
                    { null, null, driver },
                    { null, "xyz", null },
                    { null, "xyz", driver },
                    { NULL, null, null },
                    { NULL, null, driver },
                    //{ TArchiveDetector.NULL, "xyz", null },
                    { null, new Object[][] {{ "xyz", MockArchiveDriver.class }} },
                    { NULL, null },
                    { NULL, new Object[][] {{ null, null }} },
                    { NULL, new Object[][] {{ null, "" }} },
                    { NULL, new Object[][] {{ null, "xyz" }} },
                    //{ TArchiveDetector.NULL, new Object[] { "xyz", null } },
               });

        testIllegalConstructors(IllegalArgumentException.class,
                new Object[][] {
                    { "DRIVER" },
                    { "DEFAULT" },
                    { "NULL" },
                    { "ALL" },
                    { "unknownSuffix" },
                    { "", driver }, // empty suffix set
                    { ".", driver }, // empty suffix set
                    { "|", driver }, // empty suffix set
                    { "|.", driver }, // empty suffix set
                    { "||", driver }, // empty suffix set
                    { "||.", driver }, // empty suffix set
                    { "|.|", driver }, // empty suffix set
                    { "|.|.", driver }, // empty suffix set
                    { NULL, "", driver }, // empty suffix set
                    { NULL, ".", driver }, // empty suffix set
                    { NULL, "|", driver }, // empty suffix set
                    { NULL, "|.", driver }, // empty suffix set
                    { NULL, "||", driver }, // empty suffix set
                    { NULL, "||.", driver }, // empty suffix set
                    { NULL, "|.|", driver }, // empty suffix set
                    { NULL, "|.|.", driver }, // empty suffix set
                    { NULL, new Object[][] {{ "", driver }} }, // empty suffix set
                    { NULL, new Object[][] {{ ".", driver }} }, // empty suffix set
                    { NULL, new Object[][] {{ "|", driver }} }, // empty suffix set
                    { NULL, new Object[][] {{ "|.", driver }} }, // empty suffix set
                    { NULL, new Object[][] {{ "||", driver }} }, // empty suffix set
                    { NULL, new Object[][] {{ "||.", driver }} }, // empty suffix set
                    { NULL, new Object[][] {{ "|.|", driver }} }, // empty suffix set
                    { NULL, new Object[][] {{ "|.|.", driver }} }, // empty suffix set
                    { NULL, new Object[][] {{ "anySuffix", "" }} }, // empty class name
                    { NULL, new Object[][] {{ "anySuffix", "xyz" }} }, // not a class name
                    { NULL, new Object[][] {{ MockArchiveDriver.class, driver }} }, // not a suffix list
                    { NULL, new Object[][] {{ driver, driver }} }, // not a suffix list
                    { NULL, new Object[][] {{ "anySuffix", new Object() }} }, // not an archive driver
                    { NULL, new Object[][] {{ "anySuffix", Object.class }} }, // not an archive driver class
                });
    }

    @SuppressWarnings({"unchecked", "ResultOfObjectAllocationIgnored"})
    private void testIllegalConstructors(
            final Class<? extends Throwable> expected,
            final Object[][] list) {
        for (int i = 0; i < list.length; i++) {
            final Object[] args = list[i];
            Object arg0 = args[0];
            Object arg1 = null;
            Object arg2 = null;
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
            } catch (Throwable ex) {
                assertTrue(expected.isAssignableFrom(ex.getClass()));
            }
        }
    }

    @Test
    public void testGetSuffixes() {
        assertSuffixes(new String[] {
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

            "jar|zip", "JAR|ZIP",
            "jar|zip", "ZIP|JAR",
            "jar|zip", "|ZIP|JAR",
            "jar|zip", "ZIP|JAR|",
            "jar|zip", "|ZIP|JAR|",
            "jar|zip", "||ZIP|JAR|",
            "jar|zip", "|ZIP||JAR|",
            "jar|zip", "|ZIP|JAR||",

            "jar|zip", ".JAR|.ZIP",
            "jar|zip", ".ZIP|.JAR",
            "jar|zip", "|.ZIP|.JAR",
            "jar|zip", ".ZIP|.JAR|",
            "jar|zip", "|.ZIP|.JAR|",
            "jar|zip", "||.ZIP|.JAR|",
            "jar|zip", "|.ZIP||.JAR|",
            "jar|zip", "|.ZIP|.JAR||",
        });
    }

    private void assertSuffixes(final String[] args) {
        for (int i = 0; i < args.length; i++) {
            final String result = args[i++];
            final String suffixes = args[i];
            TArchiveDetector
            detector = new TArchiveDetector(suffixes, driver);
            assertEquals(result, detector.toString());
            detector = new TArchiveDetector(NULL, suffixes, driver);
            assertEquals(result, detector.toString());
            detector = new TArchiveDetector(NULL, new Object[][] {{ suffixes, driver }});
            assertEquals(result, detector.toString());
        }
    }

    @Test
    public void testNullMapping() {
        for (TArchiveDetector delegate : new TArchiveDetector[] {
            NULL,
            ALL,
        }) {
            TArchiveDetector detector = new TArchiveDetector(
                    delegate, "zip", null); // remove zip suffix
            assertFalse(new SuffixSet(detector.toString()).contains("zip"));
            detector = new TArchiveDetector(
                    delegate, ".ZIP", null); // remove zip suffix
            assertFalse(new SuffixSet(detector.toString()).contains("zip"));
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
            { null, ".null" },
            { null, ".z" },
            { null, "test" },
            { null, "test." },
            { null, "test.all" },
            { null, "test.default" },
            { null, "test.null" },
            { null, "test.z" },
        }, NULL, ALL);

        assertScheme(new String[][] {
            { null, ".jar" },
            { null, ".zip" },
            { null, "test.jar" },
            { null, "test.zip" },
        }, NULL);

        assertScheme(new String[][] {
            { "jar", ".jar" },
            { "jar", "test.jar" },
            { "zip", ".zip" },
            { "zip", "test.zip" },
        }, ALL);
    }

    private void assertScheme(
            final String[][] tests,
            final TArchiveDetector... detectors) {
        for (TArchiveDetector detector : detectors) {
            try {
                detector.getScheme(null);
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

        assertThat(detector.getScheme(lpath), equalTo(scheme));
        assertThat(detector.getScheme(upath), equalTo(scheme));
    }
}
