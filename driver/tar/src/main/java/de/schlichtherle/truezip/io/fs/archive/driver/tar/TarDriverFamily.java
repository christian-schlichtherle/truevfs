/*
 * Copyright 2010 Schlichtherle IT Services
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
package de.schlichtherle.truezip.io.fs.archive.driver.tar;

import de.schlichtherle.truezip.io.fs.FsDriver;
import de.schlichtherle.truezip.io.fs.FsDriverProvider;
import de.schlichtherle.truezip.io.fs.FsScheme;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A provider for the family of TAR drivers.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public final class TarDriverFamily implements FsDriverProvider {

    private final Map<FsScheme, FsDriver> drivers;

    public TarDriverFamily() {
        final Map<FsScheme, FsDriver> drivers = new HashMap<FsScheme, FsDriver>();
        drivers.put(FsScheme.create("tar"), new TarDriver());
        drivers.put(FsScheme.create("tgz"), new TarGZipDriver());
        drivers.put(FsScheme.create("tar.gz"), new TarGZipDriver());
        drivers.put(FsScheme.create("tbz2"), new TarBZip2Driver());
        drivers.put(FsScheme.create("tar.bz2"), new TarBZip2Driver());
        this.drivers = Collections.unmodifiableMap(drivers);
    }

    @Override
    public Map<FsScheme, FsDriver> getDrivers() {
        return drivers;
    }
}
