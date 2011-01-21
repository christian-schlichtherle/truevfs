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
package de.schlichtherle.truezip.fs.http;

import de.schlichtherle.truezip.fs.FsDriver;
import de.schlichtherle.truezip.fs.FsDriverService;
import de.schlichtherle.truezip.fs.FsScheme;
import de.schlichtherle.truezip.socket.IOPoolContainer;
import de.schlichtherle.truezip.util.SuffixSet;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.jcip.annotations.Immutable;

/**
 * An immutable container of a driver for the {@code http(s)} schemes.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@DefaultAnnotation(NonNull.class)
public final class HttpDriverContainer implements FsDriverService {

    private static final Map<FsScheme, HttpDriver> DRIVERS;

    static {
        final Map<FsScheme, HttpDriver>
                drivers = new HashMap<FsScheme, HttpDriver>();
        final HttpDriver
                driver = new HttpDriver(IOPoolContainer.SINGLETON.getPool());
        for (String scheme : new SuffixSet("http|https"))
            drivers.put(FsScheme.create(scheme), driver);
        DRIVERS = Collections.unmodifiableMap(drivers);
    }

    @Override
    public Map<FsScheme, ? extends FsDriver> getDrivers() {
        return DRIVERS;
    }
}
