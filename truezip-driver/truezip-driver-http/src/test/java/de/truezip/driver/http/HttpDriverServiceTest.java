/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.http;

import de.schlichtherle.truezip.fs.addr.FsScheme;
import de.schlichtherle.truezip.fs.spi.FsDriverService;
import de.schlichtherle.truezip.util.SuffixSet;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;

/**
 * @author  Christian Schlichtherle
 */
public final class HttpDriverServiceTest {

    public static final String DRIVER_LIST = "http|https";

    private FsDriverService instance;

    @Before
    public void setUp() {
        instance = new HttpDriverService();
    }

    @Test
    public void testGet() {
        for (String scheme : new SuffixSet(DRIVER_LIST))
            assertThat(instance.get().get(FsScheme.create(scheme)), notNullValue());
    }

    @Test
    public void testImmutability() {
        try {
            instance.get().remove(FsScheme.create("file"));
            fail("put");
        } catch (UnsupportedOperationException ex) {
        }
    }
}