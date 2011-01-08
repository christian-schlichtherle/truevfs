/*
 * Copyright (C) 2010 Schlichtherle IT Services
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
package de.schlichtherle.truezip.io.fs;

import java.io.IOException;

/**
 * Defines the potential options for data input operations.
 * Not all options may be supported or available for all operations and
 * certain combinations may even be illegal.
 * It's up to the particular operation to define which options are
 * supported and available.
 * If an option is not supported, it must be silently ignored.
 * If an option is not available, an {@link IOException} must be thrown.
 *
 * @see     FsOutputOption
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public enum FsInputOption {

    /**
     * Whether or not the entry data written by an output socket shall get
     * temporarily cached for subsequent access.
     * As a desired side effect, caching allows a file system controller to
     * {@link FsController#sync} the entry data to the backing storage
     * (e.g. a parent file system) while some client is still busy on reading
     * or writing the cached entry data.
     */
    CACHE
}
