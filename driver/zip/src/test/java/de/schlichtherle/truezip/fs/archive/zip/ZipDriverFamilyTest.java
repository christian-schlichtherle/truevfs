/*
 * Copyright 2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.fs.archive.zip;

import de.schlichtherle.truezip.fs.FsDriverContainer;
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
public class ZipDriverFamilyTest {

    public static final String DRIVER_LIST = "zip|ear|jar|war|odg|odp|ods|odt|otg|otp|ots|ott|odb|odf|odm|oth|exe";

    private FsDriverContainer instance;

    @Before
    public void setUp() {
        instance = new ZipDriverContainer();
    }

    @Test
    public void testGetDrivers() {
        for (String suffix : new SuffixSet(DRIVER_LIST))
            assertThat(instance.getDrivers().get(FsScheme.create(suffix)), notNullValue());
    }

    @Test
    public void testImmutability() {
        try {
            instance.getDrivers().remove(FsScheme.create("zip"));
            fail("put");
        } catch (UnsupportedOperationException ex) {
        }
    }
}
