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
package de.schlichtherle.truezip.io.archive.controller;

/**
 * Defines the available options for archive file system operations.
 * Not all available options may be applicable for all operations and
 * certain combinations may be useless or even illegal.
 * It's up to the particular operation to define which available options
 * are applicable for it and which combinations are supported.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public enum ArchiveIOOption {

    /**
     * Whether or not any missing parent directory entries within an archive
     * file shall get created automatically.
     * If set, client applications do not need to call
     * {@link ArchiveController#mkdir} to create the parent directory entries
     * of a file entry within an archive file before they can write to it.
     */
    CREATE_PARENTS,
    /**
     * Whether or not an operation is recursive.
     * This option affects only files and directories <em>below</em> the
     * operated node in the file system tree.
     */
    //RECURSIVE,
    /**
     * Whether or not a copy operation shall preserve as much attributes
     * of a file or directory entry within an archive file as possible.
     */
    PRESERVE,
    /**
     * Whether or not a write operation shall append to or replace the
     * contents of a file entry within an archive file.
     */
    APPEND,
}
