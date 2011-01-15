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

import de.schlichtherle.truezip.util.SuffixSet;
import de.schlichtherle.truezip.fs.FsDriver;
import de.schlichtherle.truezip.fs.FsDriverService;
import de.schlichtherle.truezip.fs.FsScheme;
import de.schlichtherle.truezip.socket.IOPoolContainer;
import de.schlichtherle.truezip.socket.IOPool;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.jcip.annotations.Immutable;

/**
 * An immutable container of drivers for the ZIP file format.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
public final class ZipDriverContainer implements FsDriverService {

    private static final IOPool<?> POOL = IOPoolContainer.SINGLETON.getPool();
    private static final Map<FsScheme, FsDriver> DRIVERS;

    static {
        final Map<FsScheme, FsDriver> drivers = new HashMap<FsScheme, FsDriver>();
        drivers.put(FsScheme.create("zip"), new ZipDriver(POOL));
        FsDriver driver = new JarDriver(POOL);
        for (String suffix : new SuffixSet("ear|jar|war"))
            drivers.put(FsScheme.create(suffix), driver);
        driver = new OdfDriver(POOL);
        for (String suffix : new SuffixSet("odg|odp|ods|odt|otg|otp|ots|ott|odb|odf|odm|oth"))
            drivers.put(FsScheme.create(suffix), driver);
        drivers.put(FsScheme.create("exe"), new ReadOnlySfxDriver(POOL));
        DRIVERS = Collections.unmodifiableMap(drivers);
    }

    @Override
    public Map<FsScheme, ? extends FsDriver> getDrivers() {
        return DRIVERS;
    }
}
