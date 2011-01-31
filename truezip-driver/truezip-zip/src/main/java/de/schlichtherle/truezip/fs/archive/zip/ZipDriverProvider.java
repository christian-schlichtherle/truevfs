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
import de.schlichtherle.truezip.fs.sl.FsDriverLocator;
import de.schlichtherle.truezip.fs.spi.FsDriverProvider;
import de.schlichtherle.truezip.socket.sl.IOPoolLocator;
import java.util.Map;
import net.jcip.annotations.Immutable;

/**
 * An immutable container of drivers for the ZIP file format.
 * <p>
 * When used with the service locator class {@link FsDriverLocator}, this
 * service provider class will register the following URI schemes for use with
 * the TrueZIP Kernel module and the following canonical archive file suffixes
 * for automatic detection by the TrueZIP File* module:
 * <table border="2" cellpadding="4">
 * <thead>
 * <tr>
 * <th>URI Schemes</th>
 * <th>Canonical Archive File Suffixes</th>
 * </tr>
 * </thead>
 * <tbody>
 * <tr>
 * <td>{@code zip}</td>
 * <td>{@code .zip}</td>
 * </tr>
 * <tr>
 * <td>{@code ear} | {@code jar} | {@code war}</td>
 * <td>{@code .ear} | {@code .jar} | {@code .war}</td>
 * </tr>
 * <tr>
 * <td>{@code odg} | {@code odp} | {@code ods} | {@code odt} | {@code otg} | {@code otp} | {@code ots} | {@code ott} | {@code odb} | {@code odf} | {@code odm} | {@code oth}</td>
 * <td>{@code .odg} | {@code .odp} | {@code .ods} | {@code .odt} | {@code .otg} | {@code .otp} | {@code .ots} | {@code .ott} | {@code .odb} | {@code .odf} | {@code .odm} | {@code .oth}</td>
 * </tr>
 * <tr>
 * <td>{@code exe}</td>
 * <td>{@code .exe}</td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
public final class ZipDriverProvider extends FsDriverProvider {

    private static final Map<FsScheme, FsDriver>
            DRIVERS = newMap(new Object[][] {
                { "zip", new ZipDriver(IOPoolLocator.SINGLETON) },
                { "ear|jar|war", new JarDriver(IOPoolLocator.SINGLETON) },
                { "odg|odp|ods|odt|otg|otp|ots|ott|odb|odf|odm|oth", new OdfDriver(IOPoolLocator.SINGLETON) },
                { "exe", new ReadOnlySfxDriver(IOPoolLocator.SINGLETON) },
            });

    @Override
    public Map<FsScheme, FsDriver> getDrivers() {
        return DRIVERS;
    }
}
