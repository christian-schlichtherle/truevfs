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

import de.schlichtherle.truezip.fs.FsDriver;
import de.schlichtherle.truezip.fs.FsScheme;
import de.schlichtherle.truezip.fs.spi.FsDriverProvider;
import de.schlichtherle.truezip.socket.sl.IOPoolLocator;
import java.util.Map;
import net.jcip.annotations.Immutable;

/**
 * An immutable container of the drivers for the TAR file format.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
public final class TarDriverProvider extends FsDriverProvider {

    private static final Map<FsScheme, FsDriver>
            DRIVERS = newMap(new Object[][] {
                { "tar", new TarDriver(IOPoolLocator.SINGLETON) },
                { "tgz|tar.gz", new TarGZipDriver(IOPoolLocator.SINGLETON) },
                { "tbz2|tar.bz2", new TarBZip2Driver(IOPoolLocator.SINGLETON) },
            });

    @Override
    public Map<FsScheme, FsDriver> getDrivers() {
        return DRIVERS;
    }
}
