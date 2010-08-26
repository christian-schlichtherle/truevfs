/*
 * Copyright (C) 2004-2010 Schlichtherle IT Services
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

import de.schlichtherle.truezip.io.archive.Archive;
import java.io.IOException;

/**
 * Indicates that the target file of an archive controller is a false
 * positive archive file which actually exists as a plain file in an
 * enclosing archive file.
 * <p>
 * Instances of this class are always associated with an
 * {@code IOException} as their cause.
 */
public final class FileArchiveEntryFalsePositiveException
extends ArchiveEntryFalsePositiveException {

    private static final long serialVersionUID = 2846364592164215345L;

    FileArchiveEntryFalsePositiveException(Archive archive, ArchiveController enclController, String enclEntryName, IOException cause) {
        super(archive, enclController, enclEntryName, cause);
    }
}
