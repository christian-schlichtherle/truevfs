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
package de.schlichtherle.truezip.fs;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * A service for a file system manager.
 * <p>
 * Implementations must be thread-safe.
 *
 * @author Christian Schlichtherle
 * @version $Id: FsManagers$
 */
public interface FsManagerService {

    /**
     * Returns the file system manager.
     * <p>
     * Calling this method multiple times must return the same file system
     * manager in order to ensure consistency of the virtual file
     * system space.
     *
     * @return The file system manager.
     */
    @NonNull FsManager getFsManager();
}
