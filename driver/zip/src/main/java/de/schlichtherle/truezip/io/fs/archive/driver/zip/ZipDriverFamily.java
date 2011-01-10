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
package de.schlichtherle.truezip.io.fs.archive.driver.zip;

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
public final class ZipDriverFamily implements FsDriverProvider {

    private final Map<FsScheme, FsDriver> drivers;

    public ZipDriverFamily() {
        final Map<FsScheme, FsDriver> drivers = new HashMap<FsScheme, FsDriver>();
        drivers.put(FsScheme.create("zip"), new ZipDriver());
        drivers.put(FsScheme.create("ear"), new JarDriver());
        drivers.put(FsScheme.create("jar"), new JarDriver());
        drivers.put(FsScheme.create("war"), new JarDriver());
        drivers.put(FsScheme.create("odg"), new OdfDriver());
        drivers.put(FsScheme.create("odp"), new OdfDriver());
        drivers.put(FsScheme.create("ods"), new OdfDriver());
        drivers.put(FsScheme.create("odt"), new OdfDriver());
        drivers.put(FsScheme.create("otg"), new OdfDriver());
        drivers.put(FsScheme.create("otp"), new OdfDriver());
        drivers.put(FsScheme.create("ots"), new OdfDriver());
        drivers.put(FsScheme.create("ott"), new OdfDriver());
        drivers.put(FsScheme.create("odb"), new OdfDriver());
        drivers.put(FsScheme.create("odf"), new OdfDriver());
        drivers.put(FsScheme.create("odm"), new OdfDriver());
        drivers.put(FsScheme.create("oth"), new OdfDriver());
        drivers.put(FsScheme.create("exe"), new ReadOnlySfxDriver());
        this.drivers = Collections.unmodifiableMap(drivers);
    }

    @Override
    public Map<FsScheme, FsDriver> getDrivers() {
        return drivers;
    }
}
