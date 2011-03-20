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

import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Map;
import javax.inject.Provider;

/**
 * A provider for an immutable map of file system schemes to drivers.
 * <p>
 * Implementations must be thread-safe.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public interface FsDriverProvider extends Provider<Map<FsScheme, FsDriver>> {

    /**
     * Returns an immutable map of file system schemes to drivers.
     * While the key of the returned map need not be {@code null},
     * its values must be nullable.
     * <p>
     * Calling this method several times should return a map which compares
     * {@link Object#equals equal} to each other in order to ensure a
     * consistent file system implementation scheme.
     *
     * @return An immutable map of file system schemes to drivers.
     */
    @Override
    @NonNull Map<FsScheme, FsDriver> get();
}
