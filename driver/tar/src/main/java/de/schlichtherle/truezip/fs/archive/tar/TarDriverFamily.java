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
package de.schlichtherle.truezip.fs.archive.tar;

import de.schlichtherle.truezip.util.SuffixSet;
import de.schlichtherle.truezip.fs.FsDriver;
import de.schlichtherle.truezip.fs.FsDriverProvider;
import de.schlichtherle.truezip.fs.FsScheme;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A provider for the family of TAR file drivers.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public final class TarDriverFamily implements FsDriverProvider {

    private final Map<FsScheme, FsDriver> drivers;

    public TarDriverFamily() {
        final Map<FsScheme, FsDriver> drivers = new HashMap<FsScheme, FsDriver>();
        drivers.put(FsScheme.create("tar"), new TarDriver());
        FsDriver driver = new TarGZipDriver();
        for (String suffix : new SuffixSet("tgz|tar.gz"))
            drivers.put(FsScheme.create(suffix), driver);
        driver = new TarBZip2Driver();
        for (String suffix : new SuffixSet("tbz2|tar.bz2"))
            drivers.put(FsScheme.create(suffix), driver);
        this.drivers = Collections.unmodifiableMap(drivers);
    }

    @Override
    public Map<FsScheme, FsDriver> getDrivers() {
        return drivers;
    }
}
