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
package de.schlichtherle.truezip.fs.spi;

import de.schlichtherle.truezip.fs.DummyDriver;
import de.schlichtherle.truezip.fs.FsDriver;
import de.schlichtherle.truezip.fs.FsScheme;
import java.util.Map;

/**
 * A service for the dummy driver.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public final class DummyDriverService extends FsDriverService {

    private final Map<FsScheme, FsDriver> drivers;

    public DummyDriverService(String suffixes) {
        this.drivers = newMap(new Object[][] {
            { suffixes, new DummyDriver() },
        });
    }

    @Override
    public Map<FsScheme, FsDriver> get() {
        return drivers;
    }
}
