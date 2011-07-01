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
import de.schlichtherle.truezip.fs.spi.FsDriverService;
import de.schlichtherle.truezip.socket.sl.IOPoolLocator;
import java.util.Map;
import net.jcip.annotations.Immutable;

/**
 * An immutable container of a map of drivers for the TAR file format.
 * The map provided by this service consists of the following entries:
 * <p>
<table border=1 cellpadding=5 summary="">
<thead>
<tr>
<th>URI Schemes</th>
<th>File System Driver Class</th>
</tr>
</thead>
<tbody>
<tr>
<td>{@code tar}</td>
<td>{@link de.schlichtherle.truezip.fs.archive.tar.TarDriver}</td>
</tr>
<tr>
<td>{@code tar.bz2}, {@code tb2}, {@code tbz}</td>
<td>{@link de.schlichtherle.truezip.fs.archive.tar.TarBZip2Driver}</td>
</tr>
<tr>
<td>{@code tar.gz}, {@code tgz}</td>
<td>{@link de.schlichtherle.truezip.fs.archive.tar.TarGZipDriver}</td>
</tr>
</tbody>
</table>
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
public final class TarDriverService extends FsDriverService {

    private static final Map<FsScheme, FsDriver>
            DRIVERS = newMap(new Object[][] {
                { "tar", new TarDriver(IOPoolLocator.SINGLETON) },
                { "tar.gz|tgz", new TarGZipDriver(IOPoolLocator.SINGLETON) },
                { "tar.bz2|tb2|tbz", new TarBZip2Driver(IOPoolLocator.SINGLETON) },
            });

    @Override
    public Map<FsScheme, FsDriver> get() {
        return DRIVERS;
    }
}
