/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip;

import de.truezip.kernel.addr.FsScheme;
import de.truezip.driver.zip.ZipDriverService;
import de.truezip.kernel.spi.FsDriverService;
import de.truezip.kernel.util.SuffixSet;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Christian Schlichtherle
 */
public class ZipDriverServiceTest {

    public static final String DRIVER_LIST = "zip|ear|jar|war|odg|odp|ods|odt|otg|otp|ots|ott|odb|odf|odm|oth|exe";

    private FsDriverService instance;

    @Before
    public void setUp() {
        instance = new ZipDriverService();
    }

    @Test
    public void testGet() {
        for (String scheme : new SuffixSet(DRIVER_LIST))
            assertThat(instance.get().get(FsScheme.create(scheme)), notNullValue());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testImmutability() {
        instance.get().remove(FsScheme.create("zip"));
    }
}
