/*
 * Copyright (C) 2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.socket;

import edu.umd.cs.findbugs.annotations.NonNull;
import javax.inject.Provider;

/**
 * A provider for an I/O pool.
 * <p>
 * Implementations must be thread-safe.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public interface IOPoolProvider extends Provider<IOPool<?>> {

    /**
     * Returns an I/O pool.
     * <p>
     * Calling this method several times may return different I/O pools,
     * so callers might need to cache the result for subsequent use.
     *
     * @return An I/O pool.
     */
    @Override
    @NonNull IOPool<?> get();
}
