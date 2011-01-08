/*
 * Copyright (C) 2005-2010 Schlichtherle IT Services
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
package de.schlichtherle.truezip.io.file;

import de.schlichtherle.truezip.io.fs.archive.driver.DummyArchiveDriver;
import de.schlichtherle.truezip.io.fs.archive.driver.ArchiveDriver;
import de.schlichtherle.truezip.io.fs.FSScheme1;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Locale;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static de.schlichtherle.truezip.io.file.ArchiveDetector.NULL;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class DefaultArchiveDetectorTest {

    private static final ArchiveDriver<?> DRIVER = new DummyArchiveDriver();
    private DefaultArchiveDetector detector;

    @Before
    public void setUp() {
        detector = new DefaultArchiveDetector(
                "ear|exe|jar|odb|odf|odg|odm|odp|ods|odt|otg|oth|otp|ots|ott|tar|tar.bz2|tar.gz|tbz2|tgz|tzp|war|zip|zip.rae|zip.raes",
                DRIVER);
    }

    @Test
    public void testDefaultConfiguration() {
        assertTrue(NULL.getSuffixes().isEmpty());
        //assertTrue(!ALL.getSuffixes().isEmpty()); // depends on run time class path configuration
    }

    @Test
    public void testIllegalConstructors() {
        testIllegalConstructors(NullPointerException.class,
                new Object[][] {
                    { null, null },
                    { null, DRIVER },
                    //{ "xyz", null },
                    { null, null, null },
                    { null, null, DRIVER },
                    { null, "xyz", null },
                    { null, "xyz", DRIVER },
                    { NULL, null, null },
                    { NULL, null, DRIVER },
                    //{ ArchiveDetector.NULL, "xyz", null },
                    { null, new Object[] { "xyz", DummyArchiveDriver.class } },
                    { NULL, null },
                    { NULL, new Object[] { null, null } },
                    { NULL, new Object[] { null, "xyz" } },
                    //{ ArchiveDetector.NULL, new Object[] { "xyz", null } },
        });

        testIllegalConstructors(IllegalArgumentException.class,
                new Object[][] {
                    { "DRIVER" },
                    { "DEFAULT" },
                    { "NULL" },
                    { "ALL" },
                    { "unknownSuffix" },
                    { "DRIVER", DRIVER }, // illegal keyword
                    { "", DRIVER }, // empty suffix set
                    { ".", DRIVER }, // empty suffix set
                    { "|", DRIVER }, // empty suffix set
                    { "|.", DRIVER }, // empty suffix set
                    { "||", DRIVER }, // empty suffix set
                    { "||.", DRIVER }, // empty suffix set
                    { "|.|", DRIVER }, // empty suffix set
                    { "|.|.", DRIVER }, // empty suffix set
                    { NULL, "DRIVER", DRIVER }, // illegal keyword
                    { NULL, "", DRIVER }, // empty suffix set
                    { NULL, ".", DRIVER }, // empty suffix set
                    { NULL, "|", DRIVER }, // empty suffix set
                    { NULL, "|.", DRIVER }, // empty suffix set
                    { NULL, "||", DRIVER }, // empty suffix set
                    { NULL, "||.", DRIVER }, // empty suffix set
                    { NULL, "|.|", DRIVER }, // empty suffix set
                    { NULL, "|.|.", DRIVER }, // empty suffix set
                    { NULL, new Object[] { "DRIVER", DRIVER } }, // illegal keyword
                    { NULL, new Object[] { "", DRIVER } }, // empty suffix set
                    { NULL, new Object[] { ".", DRIVER } }, // empty suffix set
                    { NULL, new Object[] { "|", DRIVER } }, // empty suffix set
                    { NULL, new Object[] { "|.", DRIVER } }, // empty suffix set
                    { NULL, new Object[] { "||", DRIVER } }, // empty suffix set
                    { NULL, new Object[] { "||.", DRIVER } }, // empty suffix set
                    { NULL, new Object[] { "|.|", DRIVER } }, // empty suffix set
                    { NULL, new Object[] { "|.|.", DRIVER } }, // empty suffix set
                    { NULL, new Object[] { "anySuffix", "" } }, // empty class name
                    { NULL, new Object[] { "anySuffix", new Object() } }, // not an archive driver
                    { NULL, new Object[] { "anySuffix", Object.class } }, // not an archive driver class
        });

        testIllegalConstructors(ClassCastException.class,
                new Object[][] {
                    { NULL, new Object[] { DummyArchiveDriver.class, DRIVER } }, // not a suffix list
                    { NULL, new Object[] { DRIVER, DRIVER } }, // not a suffix list
        });
    }

    @SuppressWarnings({"unchecked", "ResultOfObjectAllocationIgnored"})
    private void testIllegalConstructors(
            final Class<? extends Exception> expected,
            final Object[][] list) {
        for (int i = 0; i < list.length; i++) {
            final Object[] args = list[i];
            Object arg0 = args[0];
            Object arg1 = null;
            Object arg2 = null;
            try {
                switch (args.length) {
                    case 1:
                        new DefaultArchiveDetector((String) arg0);
                        fail("Index " + i);
                        break;

                    case 2:
                        arg1 = args[1];
                        if (arg0 != null) {
                            if (arg1 != null) {
                                if (arg0 instanceof String)
                                    new DefaultArchiveDetector((String) arg0, (ArchiveDriver<?>) arg1);
                                else if (arg1 instanceof Object[])
                                    new DefaultArchiveDetector((DefaultArchiveDetector) arg0, (Object[]) arg1);
                                else
                                    new DefaultArchiveDetector((DefaultArchiveDetector) arg0, (Map<String, Object>) arg1);
                                fail("Index " + i);
                            } else {
                                assert arg0 != null;
                                assert arg1 == null;
                                if (arg0 instanceof String) {
                                    new DefaultArchiveDetector((String) arg0, null);
                                    fail("Index " + i);
                                } else {
                                    try {
                                        new DefaultArchiveDetector((DefaultArchiveDetector) arg0, (Object[]) null);
                                        fail("Index " + i);
                                    } catch (Throwable failure) {
                                        assertTrue(expected.isAssignableFrom(failure.getClass()));
                                    }
                                    try {
                                        new DefaultArchiveDetector((DefaultArchiveDetector) arg0, (Map<String, Object>) null);
                                        fail("Index " + i);
                                    } catch (Throwable failure) {
                                        assertTrue(expected.isAssignableFrom(failure.getClass()));
                                    }
                                }
                            }
                        } else {
                            assert arg0 == null;
                            if (arg1 != null) {
                                if (arg1 instanceof ArchiveDriver<?>)
                                    new DefaultArchiveDetector(null, (ArchiveDriver<?>) arg1);
                                else if (arg1 instanceof Object[])
                                    new DefaultArchiveDetector(null, (Object[]) arg1);
                                else
                                    new DefaultArchiveDetector(null, (Map<String, Object>) arg1);
                                fail("Index " + i);
                            } else {
                                assert arg0 == null;
                                assert arg1 == null;
                                try {
                                    new DefaultArchiveDetector((String) null, (ArchiveDriver<?>) null);
                                    fail("Index " + i);
                                } catch (Throwable failure) {
                                    assertTrue(expected.isAssignableFrom(failure.getClass()));
                                }
                                try {
                                    new DefaultArchiveDetector((DefaultArchiveDetector) null, (Object[]) null);
                                    fail("Index " + i);
                                } catch (Throwable failure) {
                                    assertTrue(expected.isAssignableFrom(failure.getClass()));
                                }
                                try {
                                    new DefaultArchiveDetector((DefaultArchiveDetector) null, (Map<String, Object>) null);
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
                        new DefaultArchiveDetector((DefaultArchiveDetector) arg0, (String) arg1, (ArchiveDriver<?>) arg2);
                        fail("Index " + i);
                        break;

                    default:
                        throw new AssertionError();
                }
            } catch (Exception ex) {
                assertTrue(expected.isAssignableFrom(ex.getClass()));
            }
        }
    }

    @Test
    public void testGetSuffixes() {
        testGetSuffixes(new String[] {
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

    private void testGetSuffixes(final String[] args) {
        for (int i = 0; i < args.length; i++) {
            final String result = args[i++];
            final String suffixes = args[i];
            DefaultArchiveDetector
            detector = new DefaultArchiveDetector(suffixes, DRIVER);
            assertEquals(result, detector.getSuffixes());
            detector = new DefaultArchiveDetector(NULL, suffixes, DRIVER);
            assertEquals(result, detector.getSuffixes());
            detector = new DefaultArchiveDetector(NULL, new Object[] { suffixes, DRIVER });
            assertEquals(result, detector.getSuffixes());
        }
    }

    @Test
    public void testGetSuffixesForNullDrivers() {
        DefaultArchiveDetector detector = new DefaultArchiveDetector(
                NULL, "zip", null); // remove zip suffix
        assertEquals("", detector.getSuffixes());
        detector = new DefaultArchiveDetector(
                NULL, ".ZIP", null); // remove zip suffix
        assertEquals("", detector.getSuffixes());
    }

    @Test
    public void testGetArchiveDriver() {
        testDefaultArchiveDetector(NULL, new Object[] {
            null, "",
            null, ".",
            null, ".all",
            null, ".default",
            null, ".ear",
            null, ".exe",
            null, ".jar",
            null, ".null",
            null, ".tar",
            null, ".tar.bz2",
            null, ".tar.gz",
            null, ".tbz2",
            null, ".tgz",
            null, ".tzp",
            null, ".war",
            null, ".z",
            null, ".zip",
            null, ".zip.rae",
            null, ".zip.raes",
            null, "test",
            null, "test.",
            null, "test.all",
            null, "test.default",
            null, "test.ear",
            null, "test.exe",
            null, "test.jar",
            null, "test.null",
            null, "test.tar",
            null, "test.tar.bz2",
            null, "test.tar.gz",
            null, "test.tbz2",
            null, "test.tgz",
            null, "test.tzp",
            null, "test.war",
            null, "test.z",
            null, "test.zip",
            null, "test.zip.rae",
            null, "test.zip.raes",
        });

        testDefaultArchiveDetector(detector, new Object[] {
            null, "",
            null, ".",
            null, ".all",
            null, ".default",
            DRIVER, ".ear",
            DRIVER, ".exe",
            DRIVER, ".jar",
            null, ".null",
            DRIVER, ".tar",
            DRIVER, ".tar.bz2",
            DRIVER, ".tar.gz",
            DRIVER, ".tbz2",
            DRIVER, ".tgz",
            DRIVER, ".tzp",
            DRIVER, ".war",
            null, ".z",
            DRIVER, ".zip",
            DRIVER, ".zip.rae",
            DRIVER, ".zip.raes",
            null, "test",
            null, "test.",
            null, "test.all",
            null, "test.default",
            DRIVER, "test.ear",
            DRIVER, "test.exe",
            DRIVER, "test.jar",
            null, "test.null",
            DRIVER, "test.tar",
            DRIVER, "test.tar.bz2",
            DRIVER, "test.tar.gz",
            DRIVER, "test.tbz2",
            DRIVER, "test.tgz",
            DRIVER, "test.tzp",
            DRIVER, "test.war",
            null, "test.z",
            DRIVER, "test.zip",
            DRIVER, "test.zip.rae",
            DRIVER, "test.zip.raes",
        });
    }

    @SuppressWarnings("deprecation")
    private void testDefaultArchiveDetector(
            DefaultArchiveDetector detector,
            final Object[] args) {
        try {
            detector.getScheme(null);
            fail("Expected NullPointerException!");
        } catch (NullPointerException expected) {
        }

        try {
            detector.getDriver(null);
            fail("Expected NullPointerException!");
        } catch (NullPointerException expected) {
        }

        try {
            detector.getArchiveDriver(null);
            fail("Expected NullPointerException!");
        } catch (NullPointerException expected) {
        }

        try {
            detector = new DefaultArchiveDetector(detector, new Object[] {
                "foo", "java.lang.Object",
                "bar", "java.io.FilterInputStream",
            });
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
        }

        for (int i = 0; i < args.length; i++) {
            final ArchiveDriver<?> result = (ArchiveDriver<?>) args[i++];
            final String path = (String) args[i];
            testDefaultArchiveDetector(detector, result, path);

            // Add level of indirection in order to test caching.
            detector = new DefaultArchiveDetector(detector, new Object[0]);
            testDefaultArchiveDetector(detector, result, path);
        }
    }

    @SuppressWarnings("deprecation")
    private void testDefaultArchiveDetector(
            final DefaultArchiveDetector detector,
            final ArchiveDriver<?> result,
            final String path) {
        final String lpath = path.toLowerCase(Locale.ENGLISH);
        final String upath = path.toUpperCase(Locale.ENGLISH);

        ArchiveDriver<?> driver;
        driver = detector.getArchiveDriver(lpath);
        assertThat(driver, sameInstance((Object) result));

        driver = detector.getArchiveDriver(upath);
        assertThat(driver, sameInstance((Object) result));

        final FSScheme1 scheme = detector.getScheme(lpath);
        if (null != driver) {
            assertThat(scheme, notNullValue());
            assertThat(scheme, equalTo(detector.getScheme(upath)));
            assertThat(detector.getDriver(scheme), sameInstance((Object) result));
        } else {
            assertThat(scheme, nullValue());
        }
    }

    @Test
    public void testSerialization() throws IOException, ClassNotFoundException {
        // Serialize.
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final ObjectOutputStream out = new ObjectOutputStream(bos);
        out.writeObject(detector);
        out.close();

        // Deserialize.
        final ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        final ObjectInputStream in = new ObjectInputStream(bis);
        final DefaultArchiveDetector detector2 = (DefaultArchiveDetector) in.readObject();
        in.close();

        // Test result.
        assertNotSame(detector, detector2);
        assertNotSame(detector.getSuffixes(), detector2.getSuffixes());
        final DummyArchiveDriver driver
                = (DummyArchiveDriver) detector.getDriver(detector.getScheme("foo.zip"));
        final DummyArchiveDriver driver2
                = (DummyArchiveDriver) detector2.getDriver(detector.getScheme("bar.zip"));
        assertNotNull(driver);
        assertNotNull(driver2);
        assertNotSame(driver, driver2);
    }
}
