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
package de.schlichtherle.truezip.io.filesystem;

import java.io.IOException;

/**
 * Defines the potential options for data output operations.
 * Not all options may be supported or available for all operations and
 * certain combinations may even be illegal.
 * It's up to the particular operation to define which options are
 * supported and available.
 * If an option is not supported, it must be silently ignored.
 * If an option is not available, an {@link IOException} must be thrown.
 *
 * @see     InputOption
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public enum OutputOption {

    /**
     * Whether or not the output socket contents shall get buffered in a
     * temporary file for subsequent access.
     * As a desired side effect, buffering allows a file system controller to
     * {@link FileSystemController#sync} its contents to its underlying storage
     * while some client is still busy on reading or writing the buffer.
     */
    BUFFER,

    /**
     * Whether or not any missing parent directory entries shall get created
     * automatically.
     */
    CREATE_PARENTS,

    /**
     * Whether or not the new data shall get appended to the existing data
     * of the local target rather than replacing it entirely.
     */
    APPEND,
}
