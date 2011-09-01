/*
 * Copyright 2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.http;

import de.schlichtherle.truezip.fs.spi.FsDriverService;
import de.schlichtherle.truezip.fs.FsScheme;
import de.schlichtherle.truezip.util.SuffixSet;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import static org.hamcrest.CoreMatchers.*;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
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
