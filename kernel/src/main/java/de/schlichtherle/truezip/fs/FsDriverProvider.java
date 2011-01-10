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
package de.schlichtherle.truezip.fs;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Map;
import net.jcip.annotations.Immutable;

/**
 * A provider for an immutable map of file system schemes to file system
 * drivers.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
public interface FsDriverProvider {

    //FsDriverProvider ALL = FsClassPathDriverProvider.INSTANCE;

    /**
     * Returns an immutable map of the supported file system drivers.
     * While the key of the returned map need not be {@code null},
     * its values must be nullable.
     *
     * @return An immutable map of the supported file system drivers.
     */
    @NonNull Map<FsScheme, ? extends FsDriver> getDrivers();
}
