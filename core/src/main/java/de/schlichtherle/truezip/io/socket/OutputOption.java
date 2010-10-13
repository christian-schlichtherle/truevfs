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
package de.schlichtherle.truezip.io.socket;

import de.schlichtherle.truezip.io.filesystem.FileSystemController;
import java.io.IOException;

/**
 * Defines the potential options for data output operations.
 * Not all options may be supported or available for all operations and
 * certain combinations may even be illegal.
 * It's up to the particular operation to define which options are
 * supported and available.
 * If an option is not supported, it must be silently ignored.
 * If an option is not available, an {@link IOException} must be thrown.
 */
public enum OutputOption {

    /**
     * Whether or not any missing parent directory entries within an
     * archive file shall get created automatically.
     * If set, client applications do not need to call
     * {@link FileSystemController#mknod} to create the parent directory
     * entries of a file entry within an archive file before they can write
     * to it.
     */
    CREATE_PARENTS,

    /**
     * Whether or not the new data shall get written to a temporary file
     * for buffering if the archive entry already exists.
     * Use this option if the archive entry may be read concurrently while
     * you are writing to it.
     * The temporary file will get renamed or copied to the archive entry
     * and deleted when writing the new data has been finished.
     */
    BUFFER,

    /**
     * Whether or not the new data shall get appended to the existing data
     * of the file entry rather than replacing it entirely.
     */
    APPEND,

    /**
     * Whether or not a copy operation shall preserve the properties
     * of the source entry - but not it's name, of course.
     */
    COPY_PROPERTIES,
}
