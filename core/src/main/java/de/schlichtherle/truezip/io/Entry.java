/*
 * Copyright (C) 2005-2010 Schlichtherle IT Services
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

package de.schlichtherle.truezip.io;

import de.schlichtherle.truezip.io.archive.spi.*;

/**
 * A package private interface with some useful constants for archive entry
 * names.
 * Public classes <em>must not</em> implement this interface - otherwise the
 * constants become part of the public API.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 * @since TrueZIP 6.0
 */
interface Entry {

    /** The entry name separator as a string. */
    String SEPARATOR = ArchiveEntry.SEPARATOR;

    /** The entry name separator as a character. */
    char SEPARATOR_CHAR = ArchiveEntry.SEPARATOR_CHAR;

    /**
     * Denotes the entry name of the virtual root directory.
     * This name is used as the value of the {@code innerEntryName}
     * property if a {@code File} instance denotes an archive file.
     * <p>
     * This constant may be safely used for identity comparison.
     */
    String ROOT_NAME = "";
}
