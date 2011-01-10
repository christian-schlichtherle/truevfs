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
package de.schlichtherle.truezip.fs;

import de.schlichtherle.truezip.util.SuffixSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A provider for the dummy driver.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public final class DummyDriverProvider implements FsDriverProvider {

    private final Map<FsScheme, DummyDriver> drivers;

    public DummyDriverProvider(String suffixes) {
        final Map<FsScheme, DummyDriver> drivers = new HashMap<FsScheme, DummyDriver>();
        DummyDriver driver = new DummyDriver();
        for (String suffix : new SuffixSet(suffixes))
            drivers.put(FsScheme.create(suffix), driver);
        this.drivers = Collections.unmodifiableMap(drivers);
    }

    @Override
    public Map<FsScheme, DummyDriver> getDrivers() {
        return drivers;
    }
}
