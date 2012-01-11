/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.archive.tar;

import de.schlichtherle.truezip.fs.FsScheme;
import de.schlichtherle.truezip.fs.spi.FsDriverService;
import de.schlichtherle.truezip.util.SuffixSet;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
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

    @Test
    public void testImmutability() {
        try {
            instance.get().remove(FsScheme.create("tar"));
            fail("put");
        } catch (UnsupportedOperationException expected) {
        }
    }
}
