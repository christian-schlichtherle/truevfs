/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.tar;

import de.truezip.kernel.addr.FsScheme;
import de.truezip.driver.tar.TarDriverService;
import de.truezip.kernel.fs.spi.FsDriverService;
import de.truezip.kernel.util.SuffixSet;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;

/**
 * @author  Christian Schlichtherle
 */
public class TarDriverServiceTest {

    public static final String DRIVER_LIST = "tar|tar.bz2|tar.gz|tbz|tb2|tgz";

    private FsDriverService instance;

    @Before
    public void setUp() {
        instance = new TarDriverService();
    }

    @Test
    public void testGet() {
        for (String scheme : new SuffixSet(DRIVER_LIST))
            assertThat(instance.get().get(FsScheme.create(scheme)), notNullValue());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testImmutability() {
        instance.get().remove(FsScheme.create("tar"));
    }
}