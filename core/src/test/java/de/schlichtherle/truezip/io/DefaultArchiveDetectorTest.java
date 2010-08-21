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

package de.schlichtherle.truezip.io;

import de.schlichtherle.truezip.io.archive.driver.ArchiveDriver;
import de.schlichtherle.truezip.io.archive.zip.JarDriver;
import de.schlichtherle.truezip.io.archive.zip.ZipDriver;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;
import junit.framework.TestCase;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class DefaultArchiveDetectorTest extends TestCase {

    public DefaultArchiveDetectorTest(String testName) {
        super(testName);
    }

    public void testDefaultConfiguration() {
        assertEquals("",
                ArchiveDetector.NULL.getSuffixes());
        assertEquals("ear|jar|war|zip",
                ArchiveDetector.DEFAULT.getSuffixes());
        assertEquals("ear|exe|jar|odb|odf|odg|odm|odp|ods|odt|otg|oth|otp|ots|ott|tar|tar.bz2|tar.gz|tbz2|tgz|tzp|war|zip|zip.rae|zip.raes",
                ArchiveDetector.ALL.getSuffixes());
    }

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
                    { ArchiveDetector.NULL, null, null },
                    { ArchiveDetector.NULL, null, new ZipDriver() },
                    //{ ArchiveDetector.NULL, "xyz", null },
                    { null, new Object[] { "xyz", ZipDriver.class } },
                    { ArchiveDetector.NULL, null },
                    { ArchiveDetector.NULL, new Object[] { null, null } },
                    { ArchiveDetector.NULL, new Object[] { null, "xyz" } },
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
                    { ArchiveDetector.NULL, "DRIVER", new ZipDriver() }, // illegal keyword
                    { ArchiveDetector.NULL, "DEFAULT", new ZipDriver() }, // illegal keyword
                    { ArchiveDetector.NULL, "", new ZipDriver() }, // empty suffix set
                    { ArchiveDetector.NULL, ".", new ZipDriver() }, // empty suffix set
                    { ArchiveDetector.NULL, "|", new ZipDriver() }, // empty suffix set
                    { ArchiveDetector.NULL, "|.", new ZipDriver() }, // empty suffix set
                    { ArchiveDetector.NULL, "||", new ZipDriver() }, // empty suffix set
                    { ArchiveDetector.NULL, "||.", new ZipDriver() }, // empty suffix set
                    { ArchiveDetector.NULL, "|.|", new ZipDriver() }, // empty suffix set
                    { ArchiveDetector.NULL, "|.|.", new ZipDriver() }, // empty suffix set
                    { ArchiveDetector.NULL, new Object[] { "DRIVER", new ZipDriver() } }, // illegal keyword
                    { ArchiveDetector.NULL, new Object[] { "DEFAULT", new ZipDriver() } }, // illegal keyword
                    { ArchiveDetector.NULL, new Object[] { "", new ZipDriver() } }, // empty suffix set
                    { ArchiveDetector.NULL, new Object[] { ".", new ZipDriver() } }, // empty suffix set
                    { ArchiveDetector.NULL, new Object[] { "|", new ZipDriver() } }, // empty suffix set
                    { ArchiveDetector.NULL, new Object[] { "|.", new ZipDriver() } }, // empty suffix set
                    { ArchiveDetector.NULL, new Object[] { "||", new ZipDriver() } }, // empty suffix set
                    { ArchiveDetector.NULL, new Object[] { "||.", new ZipDriver() } }, // empty suffix set
                    { ArchiveDetector.NULL, new Object[] { "|.|", new ZipDriver() } }, // empty suffix set
                    { ArchiveDetector.NULL, new Object[] { "|.|.", new ZipDriver() } }, // empty suffix set
                    { ArchiveDetector.NULL, new Object[] { "anySuffix", "" } }, // empty class name
                    { ArchiveDetector.NULL, new Object[] { ZipDriver.class, new ZipDriver() } }, // not a suffix list
                    { ArchiveDetector.NULL, new Object[] { new ZipDriver(), new ZipDriver() } }, // not a suffix list
                    { ArchiveDetector.NULL, new Object[] { "anySuffix", new Object() } }, // not an archive driver
                    { ArchiveDetector.NULL, new Object[] { "anySuffix", Object.class } }, // not an archive driver class
        });
    }

    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    private void testIllegalConstructors(
            final Class expected,
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
                                    new DefaultArchiveDetector((String) arg0, (ArchiveDriver) arg1);
                                else if (arg1 instanceof Object[])
                                    new DefaultArchiveDetector((DefaultArchiveDetector) arg0, (Object[]) arg1);
                                else
                                    new DefaultArchiveDetector((DefaultArchiveDetector) arg0, (Map) arg1);
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
                                        new DefaultArchiveDetector((DefaultArchiveDetector) arg0, (Map) null);
                                        fail("Index " + i);
                                    } catch (Throwable failure) {
                                        assertTrue(expected.isAssignableFrom(failure.getClass()));
                                    }
                                }
                            }
                        } else {
                            assert arg0 == null;
                            if (arg1 != null) {
                                if (arg1 instanceof ArchiveDriver)
                                    new DefaultArchiveDetector(null, (ArchiveDriver) arg1);
                                else if (arg1 instanceof Object[])
                                    new DefaultArchiveDetector(null, (Object[]) arg1);
                                else
                                    new DefaultArchiveDetector(null, (Map) arg1);
                                fail("Index " + i);
                            } else {
                                assert arg0 == null;
                                assert arg1 == null;
                                try {
                                    new DefaultArchiveDetector((String) null, (ArchiveDriver) null);
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
                                    new DefaultArchiveDetector((DefaultArchiveDetector) null, (Map) null);
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
                        new DefaultArchiveDetector((DefaultArchiveDetector) arg0, (String) arg1, (ArchiveDriver) arg2);
                        fail("Index " + i);
                        break;

                    default:
                        throw new AssertionError();
                }
            } catch (Exception failure) {
                assertTrue(expected.isAssignableFrom(failure.getClass()));
            }
        }
    }

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
            String result;

            DefaultArchiveDetector detector;

            detector = new DefaultArchiveDetector(suffixList);
            assertEquals(expResult, detector.getSuffixes());

            if (expResult.length() > 0) {
                detector = new DefaultArchiveDetector(suffixList, new ZipDriver());
                assertEquals(expResult, detector.getSuffixes());

                detector = new DefaultArchiveDetector(
                        ArchiveDetector.NULL, suffixList, new ZipDriver());
                assertEquals(expResult, detector.getSuffixes());

                detector = new DefaultArchiveDetector(
                        ArchiveDetector.NULL,
                        new Object[] { suffixList, new ZipDriver() });
                assertEquals(expResult, detector.getSuffixes());
            }
        }
    }

    public void testGetSuffixesForNullDrivers() {
        DefaultArchiveDetector detector = new DefaultArchiveDetector(
                ArchiveDetector.NULL, "zip", null); // remove zip suffix
        assertEquals("", detector.getSuffixes());
        detector = new DefaultArchiveDetector(
                ArchiveDetector.DEFAULT, ".ZIP", null); // remove zip suffix
        assertEquals("ear|jar|war", detector.getSuffixes());
    }

    /**
     * Test of getArchiveDriver method, of class de.schlichtherle.truezip.io.DefaultArchiveDetector.
     */
    public void testGetArchiveDriver() {
        testGetArchiveDriver(ArchiveDetector.NULL, new Object[] {
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

        final ArchiveDriver earDriver = ArchiveDetector.ALL.getArchiveDriver("test.ear");
        assertNotNull(earDriver);
        final ArchiveDriver exeDriver = ArchiveDetector.ALL.getArchiveDriver("test.exe");
        assertNotNull(exeDriver);
        final ArchiveDriver jarDriver = ArchiveDetector.ALL.getArchiveDriver("test.jar");
        assertNotNull(jarDriver);
        final ArchiveDriver tarDriver = ArchiveDetector.ALL.getArchiveDriver("test.tar");
        assertNotNull(tarDriver);
        final ArchiveDriver tarBz2Driver = ArchiveDetector.ALL.getArchiveDriver("test.tar.bz2");
        assertNotNull(tarBz2Driver);
        final ArchiveDriver tarGzDriver = ArchiveDetector.ALL.getArchiveDriver("test.tar.gz");
        assertNotNull(tarGzDriver);
        final ArchiveDriver tbz2Driver = ArchiveDetector.ALL.getArchiveDriver("test.tbz2");
        assertNotNull(tbz2Driver);
        final ArchiveDriver tgzDriver = ArchiveDetector.ALL.getArchiveDriver("test.tgz");
        assertNotNull(tgzDriver);
        final ArchiveDriver tzpDriver = ArchiveDetector.ALL.getArchiveDriver("test.tzp");
        assertNotNull(tzpDriver);
        final ArchiveDriver warDriver = ArchiveDetector.ALL.getArchiveDriver("test.war");
        assertNotNull(warDriver);
        final ArchiveDriver zipDriver = ArchiveDetector.ALL.getArchiveDriver("test.zip");
        assertNotNull(zipDriver);
        final ArchiveDriver zipRaeDriver = ArchiveDetector.ALL.getArchiveDriver("test.zip.rae");
        assertNotNull(zipRaeDriver);
        final ArchiveDriver zipRaesDriver = ArchiveDetector.ALL.getArchiveDriver("test.zip.raes");
        assertNotNull(zipRaesDriver);

        testGetArchiveDriver(ArchiveDetector.DEFAULT, new Object[] {
            null, "",
            null, ".",
            null, ".all",
            null, ".default",
            earDriver, ".ear",
            null, ".exe",
            jarDriver, ".jar",
            null, ".null",
            null, ".tar",
            null, ".tar.bz2",
            null, ".tar.gz",
            null, ".tbz2",
            null, ".tgz",
            null, ".tzp",
            warDriver, ".war",
            null, ".z",
            zipDriver, ".zip",
            null, ".zip.rae",
            null, ".zip.raes",
            null, "test",
            null, "test.",
            null, "test.all",
            null, "test.default",
            earDriver, "test.ear",
            null, "test.exe",
            jarDriver, "test.jar",
            null, "test.null",
            null, "test.tar",
            null, "test.tar.bz2",
            null, "test.tar.gz",
            null, "test.tbz2",
            null, "test.tgz",
            null, "test.tzp",
            warDriver, "test.war",
            null, "test.z",
            zipDriver, "test.zip",
            null, "test.zip.rae",
            null, "test.zip.raes",
        });

        testGetArchiveDriver(ArchiveDetector.ALL, new Object[] {
            null, "",
            null, ".",
            null, ".all",
            null, ".default",
            earDriver, ".ear",
            exeDriver, ".exe",
            jarDriver, ".jar",
            null, ".null",
            tarDriver, ".tar",
            tarBz2Driver, ".tar.bz2",
            tarGzDriver, ".tar.gz",
            tbz2Driver, ".tbz2",
            tgzDriver, ".tgz",
            tzpDriver, ".tzp",
            warDriver, ".war",
            null, ".z",
            zipDriver, ".zip",
            zipRaeDriver, ".zip.rae",
            zipRaesDriver, ".zip.raes",
            null, "test",
            null, "test.",
            null, "test.all",
            null, "test.default",
            earDriver, "test.ear",
            exeDriver, "test.exe",
            jarDriver, "test.jar",
            null, "test.null",
            tarDriver, "test.tar",
            tarBz2Driver, "test.tar.bz2",
            tarGzDriver, "test.tar.gz",
            tbz2Driver, "test.tbz2",
            tgzDriver, "test.tgz",
            tzpDriver, "test.tzp",
            warDriver, "test.war",
            null, "test.z",
            zipDriver, "test.zip",
            zipRaeDriver, "test.zip.rae",
            zipRaesDriver, "test.zip.raes",
        });
    }

    private void testGetArchiveDriver(
            DefaultArchiveDetector detector,
            final Object[] args) {
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

        // Add level of indirection in order to test caching.
        detector = new DefaultArchiveDetector(detector, new Object[] {
            "foo", new JarDriver(),
            "bar", new JarDriver(),
        });

        for (int i = 0; i < args.length; i++) {
            final ArchiveDriver expResult = (ArchiveDriver) args[i++];
            final String path = (String) args[i];
            final String lPath = path.toLowerCase();
            final String uPath = path.toUpperCase();

            ArchiveDriver driver;
            driver = detector.getArchiveDriver(lPath);
            assertSame(expResult, driver);

            driver = detector.getArchiveDriver(uPath);
            assertSame(expResult, driver);

            ArchiveDetector detector2;
            detector2 = new DefaultArchiveDetector(detector, new Object[0]);

            driver = detector2.getArchiveDriver(lPath);
            assertSame(expResult, driver);

            driver = detector2.getArchiveDriver(uPath);
            assertSame(expResult, driver);
        }
    }

    public void testSerialization() throws IOException, ClassNotFoundException {
        // Preamble.
        DefaultArchiveDetector detector = ArchiveDetector.DEFAULT;

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
                = (ZipDriver) detector.getArchiveDriver("foo.zip");
        final ZipDriver driver2
                = (ZipDriver) detector2.getArchiveDriver("bar.zip");
        assertNotNull(driver);
        assertNotNull(driver2);
        assertNotSame(driver, driver2);
    }
}
