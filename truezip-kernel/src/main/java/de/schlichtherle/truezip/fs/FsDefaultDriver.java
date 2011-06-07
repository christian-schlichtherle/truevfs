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
package de.schlichtherle.truezip.fs;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Map;
import net.jcip.annotations.Immutable;

/**
 * Uses a given file system driver service to lookup the appropriate driver
 * for the scheme of a given mount point.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@DefaultAnnotation(NonNull.class)
public final class FsDefaultDriver extends FsAbstractCompositeDriver {

    private final Map<FsScheme, FsDriver> drivers;

    /**
     * Constructs a new file system default driver which will query the given
     * file system driver provider for an appropriate file system driver for
     * the scheme of a given mount point.
     */
    public FsDefaultDriver(final FsDriverProvider provider) {
        this.drivers = provider.get(); // immutable map!
        assert null != drivers;
    }

    @Override
    public Map<FsScheme, FsDriver> get() {
        return drivers;
    }
}
