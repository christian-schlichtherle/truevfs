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

import de.schlichtherle.truezip.io.archive.ArchiveDescriptor;
import java.io.IOException;

/**
 * Indicates a false positive archive entry which actually exists as a
 * file or directory entry in an enclosing archive file.
 * <p>
 * Instances of this class are always associated with an {@code IOException}
 * as their cause.
 */
public abstract class ArchiveEntryFalsePositiveException
extends FalsePositiveException {

    private static final long serialVersionUID = 1234592343828746533L;

    ArchiveEntryFalsePositiveException(
            ArchiveDescriptor archive,
            String path,
            IOException cause) {
        super(archive, path, cause);
    }
}
