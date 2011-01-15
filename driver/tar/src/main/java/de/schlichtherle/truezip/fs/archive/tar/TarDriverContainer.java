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
package de.schlichtherle.truezip.fs.archive.tar;

import de.schlichtherle.truezip.util.SuffixSet;
import de.schlichtherle.truezip.fs.FsDriver;
import de.schlichtherle.truezip.fs.FsDriverService;
import de.schlichtherle.truezip.fs.FsScheme;
import de.schlichtherle.truezip.fs.archive.ArchiveDriver;
import de.schlichtherle.truezip.socket.IOPoolContainer;
import de.schlichtherle.truezip.socket.IOPool;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.jcip.annotations.Immutable;

/**
 * An immutable container of the drivers for the TAR file format.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
public final class TarDriverContainer implements FsDriverService {

    private static final IOPool<?> POOL = IOPoolContainer.SINGLETON.getPool();
    private static final Map<FsScheme, ArchiveDriver<?>> DRIVERS;

    static {
        final Map<FsScheme, ArchiveDriver<?>>
                drivers = new HashMap<FsScheme, ArchiveDriver<?>>();
        drivers.put(FsScheme.create("tar"), new TarDriver(POOL));
        ArchiveDriver<?> driver = new TarGZipDriver(POOL);
        for (String suffix : new SuffixSet("tgz|tar.gz"))
            drivers.put(FsScheme.create(suffix), driver);
        driver = new TarBZip2Driver(POOL);
        for (String suffix : new SuffixSet("tbz2|tar.bz2"))
            drivers.put(FsScheme.create(suffix), driver);
        DRIVERS = Collections.unmodifiableMap(drivers);
    }

    @Override
    public Map<FsScheme, ArchiveDriver<?>> getDrivers() {
        return DRIVERS;
    }
}
