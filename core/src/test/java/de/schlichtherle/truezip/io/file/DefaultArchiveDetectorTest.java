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

import de.schlichtherle.truezip.io.archive.driver.ArchiveDriver;
import de.schlichtherle.truezip.io.archive.driver.zip.JarDriver;
import de.schlichtherle.truezip.io.archive.driver.zip.ZipDriver;
import de.schlichtherle.truezip.io.filesystem.Scheme;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static de.schlichtherle.truezip.io.file.ArchiveDetector.*;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class DefaultArchiveDetectorTest {

    @Test
    public void testDefaultConfiguration() {
        assertEquals("",
                NULL.getSuffixes());
        assertEquals("ear|exe|jar|odb|odf|odg|odm|odp|ods|odt|otg|oth|otp|ots|ott|tar|tar.bz2|tar.gz|tbz2|tgz|tzp|war|zip|zip.rae|zip.raes",
                ALL.getSuffixes());
    }

    @Test
    public void testIllegalConstructors() {
        testIllegalConstructors(NullPointerException.class,
                new Object[][] {
                    { null, null },
                    { null, new ZipDriver() },
                    //{ "xyz", null },
                    { null, null, null },
                    { null, null, new ZipDriver() },
                    { null, "xyz", null },
                    { null, "xyz", new ZipDriver() },
                    { NULL, null, null },
                    { NULL, null, new ZipDriver() },
                    //{ ArchiveDetector.NULL, "xyz", null },
                    { null, new Object[] { "xyz", ZipDriver.class } },
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
                    { "DRIVER", new ZipDriver() }, // illegal keyword
                    { "DEFAULT", new ZipDriver() }, // illegal keyword
                    { "", new ZipDriver() }, // empty suffix set
                    { ".", new ZipDriver() }, // empty suffix set
                    { "|", new ZipDriver() }, // empty suffix set
                    { "|.", new ZipDriver() }, // empty suffix set
                    { "||", new ZipDriver() }, // empty suffix set
                    { "||.", new ZipDriver() }, // empty suffix set
                    { "|.|", new ZipDriver() }, // empty suffix set
                    { "|.|.", new ZipDriver() }, // empty suffix set
                    { NULL, "DRIVER", new ZipDriver() }, // illegal keyword
                    { NULL, "DEFAULT", new ZipDriver() }, // illegal keyword
                    { NULL, "", new ZipDriver() }, // empty suffix set
                    { NULL, ".", new ZipDriver() }, // empty suffix set
                    { NULL, "|", new ZipDriver() }, // empty suffix set
                    { NULL, "|.", new ZipDriver() }, // empty suffix set
                    { NULL, "||", new ZipDriver() }, // empty suffix set
                    { NULL, "||.", new ZipDriver() }, // empty suffix set
                    { NULL, "|.|", new ZipDriver() }, // empty suffix set
                    { NULL, "|.|.", new ZipDriver() }, // empty suffix set
                    { NULL, new Object[] { "DRIVER", new ZipDriver() } }, // illegal keyword
                    { NULL, new Object[] { "DEFAULT", new ZipDriver() } }, // illegal keyword
                    { NULL, new Object[] { "", new ZipDriver() } }, // empty suffix set
                    { NULL, new Object[] { ".", new ZipDriver() } }, // empty suffix set
                    { NULL, new Object[] { "|", new ZipDriver() } }, // empty suffix set
                    { NULL, new Object[] { "|.", new ZipDriver() } }, // empty suffix set
                    { NULL, new Object[] { "||", new ZipDriver() } }, // empty suffix set
                    { NULL, new Object[] { "||.", new ZipDriver() } }, // empty suffix set
                    { NULL, new Object[] { "|.|", new ZipDriver() } }, // empty suffix set
                    { NULL, new Object[] { "|.|.", new ZipDriver() } }, // empty suffix set
                    { NULL, new Object[] { "anySuffix", "" } }, // empty class name
                    { NULL, new Object[] { "anySuffix", new Object() } }, // not an archive driver
                    { NULL, new Object[] { "anySuffix", Object.class } }, // not an archive driver class
        });

        testIllegalConstructors(ClassCastException.class,
                new Object[][] {
                    { NULL, new Object[] { ZipDriver.class, new ZipDriver() } }, // not a suffix list
                    { NULL, new Object[] { new ZipDriver(), new ZipDriver() } }, // not a suffix list
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
            "", null,
            "", "",
            "", ".",
            "", "|",
            "", ".|",
            "", "|.",

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
            final String expResult = args[i++];
            final String suffixList = args[i];
            DefaultArchiveDetector detector;
            detector = new DefaultArchiveDetector(suffixList);
            assertEquals(expResult, detector.getSuffixes());
            if (expResult.length() > 0) {
                detector = new DefaultArchiveDetector(suffixList, new ZipDriver());
                assertEquals(expResult, detector.getSuffixes());
                detector = new DefaultArchiveDetector(
                        NULL, suffixList, new ZipDriver());
                assertEquals(expResult, detector.getSuffixes());
                detector = new DefaultArchiveDetector(
                        NULL,
                        new Object[] { suffixList, new ZipDriver() });
                assertEquals(expResult, detector.getSuffixes());
            }
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
        testGetArchiveDriver(NULL, new Object[] {
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

        for (final String param : new String[] {
            "test.ear",
            "test.exe",
            "test.jar",
            "test.tar",
            "test.tar.bz2",
            "test.tar.gz",
            "test.tbz2",
            "test.tgz",
            "test.tzp",
            "test.war",
            "test.zip",
            "test.zip.rae",
            "test.zip.raes",
        }) {
            final Scheme type = ALL.getScheme(param);
            assertThat(type, notNullValue());
            assertThat(ALL.getDriver(type), notNullValue());
        }

        testGetArchiveDriver(ArchiveDetector.ALL, new Object[] {
            null, "",
            null, ".",
            null, ".all",
            null, ".default",
            ALL.getDriver(Scheme.create("ear")), ".ear",
            ALL.getDriver(Scheme.create("exe")), ".exe",
            ALL.getDriver(Scheme.create("jar")), ".jar",
            null, ".null",
            ALL.getDriver(Scheme.create("tar")), ".tar",
            ALL.getDriver(Scheme.create("tar.bz2")), ".tar.bz2",
            ALL.getDriver(Scheme.create("tar.gz")), ".tar.gz",
            ALL.getDriver(Scheme.create("tbz2")), ".tbz2",
            ALL.getDriver(Scheme.create("tgz")), ".tgz",
            ALL.getDriver(Scheme.create("tzp")), ".tzp",
            ALL.getDriver(Scheme.create("war")), ".war",
            null, ".z",
            ALL.getDriver(Scheme.create("zip")), ".zip",
            ALL.getDriver(Scheme.create("zip.rae")), ".zip.rae",
            ALL.getDriver(Scheme.create("zip.raes")), ".zip.raes",
            null, "test",
            null, "test.",
            null, "test.all",
            null, "test.default",
            ALL.getDriver(Scheme.create("ear")), "test.ear",
            ALL.getDriver(Scheme.create("exe")), "test.exe",
            ALL.getDriver(Scheme.create("jar")), "test.jar",
            null, "test.null",
            ALL.getDriver(Scheme.create("tar")), "test.tar",
            ALL.getDriver(Scheme.create("tar.bz2")), "test.tar.bz2",
            ALL.getDriver(Scheme.create("tar.gz")), "test.tar.gz",
            ALL.getDriver(Scheme.create("tbz2")), "test.tbz2",
            ALL.getDriver(Scheme.create("tgz")), "test.tgz",
            ALL.getDriver(Scheme.create("tzp")), "test.tzp",
            ALL.getDriver(Scheme.create("war")), "test.war",
            null, "test.z",
            ALL.getDriver(Scheme.create("zip")), "test.zip",
            ALL.getDriver(Scheme.create("zip.rae")), "test.zip.rae",
            ALL.getDriver(Scheme.create("zip.raes")), "test.zip.raes",
        });
    }

    private void testGetArchiveDriver(
            DefaultArchiveDetector detector,
            final Object[] args) {
        try {
            detector.getDriver((String) null);
            fail("Expected NullPointerException!");
        } catch (NullPointerException expected) {
        }

        try {
            detector.getDriver((Scheme) null);
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

        // Add level of indirection in order to test caching.
        detector = new DefaultArchiveDetector(detector, new Object[] {
            "foo", new JarDriver(),
            "bar", new JarDriver(),
        });

        for (int i = 0; i < args.length; i++) {
            final ArchiveDriver<?> expResult = (ArchiveDriver<?>) args[i++];
            final String path = (String) args[i];
            final String lPath = path.toLowerCase();
            final String uPath = path.toUpperCase();

            ArchiveDriver<?> driver;
            driver = detector.getDriver(lPath);
            assertSame(expResult, driver);

            driver = detector.getDriver(uPath);
            assertSame(expResult, driver);

            DefaultArchiveDetector detector2;
            detector2 = new DefaultArchiveDetector(detector, new Object[0]);

            driver = detector2.getDriver(lPath);
            assertSame(expResult, driver);

            driver = detector2.getDriver(uPath);
            assertSame(expResult, driver);
        }
    }

    @Test
    public void testSerialization() throws IOException, ClassNotFoundException {
        // Preamble.
        DefaultArchiveDetector detector = ArchiveDetector.ALL;

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
        final ZipDriver driver
                = (ZipDriver) detector.getDriver(detector.getScheme("foo.zip"));
        final ZipDriver driver2
                = (ZipDriver) detector2.getDriver(detector.getScheme("bar.zip"));
        assertNotNull(driver);
        assertNotNull(driver2);
        assertNotSame(driver, driver2);
    }
}
