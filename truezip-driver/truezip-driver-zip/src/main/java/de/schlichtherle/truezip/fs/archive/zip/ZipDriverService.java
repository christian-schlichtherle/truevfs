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

import de.schlichtherle.truezip.fs.FsDriver;
import de.schlichtherle.truezip.fs.FsScheme;
import de.schlichtherle.truezip.fs.spi.FsDriverService;
import de.schlichtherle.truezip.socket.sl.IOPoolLocator;
import java.util.Map;
import net.jcip.annotations.Immutable;

/**
 * An immutable container of a map of drivers for the ZIP file format.
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
<td>{@code ear}, {@code jar}, {@code war}</td>
<td>{@link de.schlichtherle.truezip.fs.archive.zip.JarDriver}</td>
</tr>
<tr>
<td>{@code exe}</td>
<td>{@link de.schlichtherle.truezip.fs.archive.zip.ReadOnlySfxDriver}</td>
</tr>
<tr>
<td>{@code odb}, {@code odf}, {@code odg}, {@code odm}, {@code odp}, {@code ods}, {@code odt}, {@code otg}, {@code oth}, {@code otp}, {@code ots}, {@code ott}</td>
<td>{@link de.schlichtherle.truezip.fs.archive.zip.OdfDriver}</td>
</tr>
<tr>
<td>{@code zip}</td>
<td>{@link de.schlichtherle.truezip.fs.archive.zip.ZipDriver}</td>
</tr>
</tbody>
</table>
 * <p>
 * Note that the regular expression is actually decomposed into separate
 * {@link FsScheme} objects which get mapped individually.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
public final class ZipDriverService extends FsDriverService {

    private static final Map<FsScheme, FsDriver>
            DRIVERS = newMap(new Object[][] {
                { "zip", new ZipDriver(IOPoolLocator.SINGLETON) },
                { "ear|jar|war", new JarDriver(IOPoolLocator.SINGLETON) },
                { "odb|odf|odg|odm|odp|ods|odt|otg|oth|otp|ots|ott", new OdfDriver(IOPoolLocator.SINGLETON) },
                { "exe", new ReadOnlySfxDriver(IOPoolLocator.SINGLETON) },
            });

    @Override
    public Map<FsScheme, FsDriver> get() {
        return DRIVERS;
    }
}
