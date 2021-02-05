/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.access;

import global.namespace.truevfs.commons.shed.ExtensionSet;
import global.namespace.truevfs.driver.mock.MockArchiveDriver;
import global.namespace.truevfs.kernel.api.FsArchiveDriver;
import global.namespace.truevfs.kernel.api.FsScheme;
import lombok.val;
import org.junit.Test;

import java.io.File;
import java.util.Collections;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

/**
 * @author Christian Schlichtherle
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public final class TArchiveDetectorTest {

    private final Optional<FsArchiveDriver<?>> driver = Optional.of(new MockArchiveDriver());
    private final TArchiveDetector
            ALL = new TArchiveDetector("tar.gz|zip", driver),
            NULL = new TArchiveDetector("", ALL), // test decoration
            MOK = new TArchiveDetector("mok", driver, NULL); // test decoration

    @Test
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testIllegalConstructors() {
        val tests = new Supplier<?>[]{
                () -> new TArchiveDetector("DRIVER"),
                () -> new TArchiveDetector("DEFAULT"),
                () -> new TArchiveDetector("NULL"),
                () -> new TArchiveDetector("ALL"),
                () -> new TArchiveDetector("unknownExtension"),
                () -> new TArchiveDetector("", driver), // empty extension set
                () -> new TArchiveDetector(".", driver), // empty extension set
                () -> new TArchiveDetector("|", driver), // empty extension set
                () -> new TArchiveDetector("|.", driver), // empty extension set
                () -> new TArchiveDetector("||", driver), // empty extension set
                () -> new TArchiveDetector("||.", driver), // empty extension set
                () -> new TArchiveDetector("|.|", driver), // empty extension set
                () -> new TArchiveDetector("|.|.", driver), // empty extension set
        };
        for (val test : tests) {
            try {
                test.get();
                fail();
            } catch (final IllegalArgumentException ignored) {
            }
        }
    }

    @Test
    public void testGetExtensions() {
        assertExtensions(new String[]{
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
            assertEquals(result, detector.getExtensions());
            detector = new TArchiveDetector(extensions, driver, NULL);
            assertEquals(result, detector.getExtensions());
        }
    }

    @Test
    public void testNullMapping() {
        for (TArchiveDetector delegate : new TArchiveDetector[]{
                NULL,
                ALL,
        }) {
            TArchiveDetector detector = new TArchiveDetector("zip", Optional.empty(), delegate); // remove zip extension
            assertFalse(new ExtensionSet(detector.getExtensions()).contains("zip"));
            detector = new TArchiveDetector(".ZIP", Optional.empty(), delegate); // remove zip extension
            assertFalse(new ExtensionSet(detector.getExtensions()).contains("zip"));
        }
    }

    @Test
    public void testGetDriver() {
        assertScheme(new String[][]{
                {null, ""},
                {null, "."},
                {null, ".all"},
                {null, ".default"},
                {null, ".ear"},
                {null, ".exe"},
                {null, ".file"},
                {null, ".null"},
                {null, ".z"},
                {null, "test"},
                {null, "test."},
                {null, "test.all"},
                {null, "test.default"},
                {null, "test.null"},
                {null, "test.z"},
        }, NULL, MOK, ALL);

        assertScheme(new String[][]{
                {null, ".tar.gz"},
                {null, ".zip"},
                {null, "test.tar.gz"},
                {null, "test.zip"},
        }, NULL, MOK);

        assertScheme(new String[][]{
                {"tar.gz", ".tar.gz"},
                {"tar.gz", "test.tar.gz"},
                {"tar.gz", "foo" + File.separator + "test.123.tar.gz"},
                {"zip", ".zip"},
                {"zip", "test.zip"},
                {"zip", "foo" + File.separator + "test.123.zip"},
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
                final Optional<FsScheme> scheme = Optional.ofNullable(test[0]).map(FsScheme::create);
                final String path = test[1];
                assertScheme(detector, scheme, path);

                // Add level of indirection in order to test caching.
                detector = new TArchiveDetector(Collections.emptyMap(), detector);
                assertScheme(detector, scheme, path);
            }
        }
    }

    private void assertScheme(
            final TArchiveDetector detector,
            final Optional<FsScheme> scheme,
            final String path) {
        final String lpath = path.toLowerCase(Locale.ROOT);
        final String upath = path.toUpperCase(Locale.ROOT);

        assertThat(detector.scheme(lpath), equalTo(scheme));
        assertThat(detector.scheme(upath), equalTo(scheme));
    }
}
