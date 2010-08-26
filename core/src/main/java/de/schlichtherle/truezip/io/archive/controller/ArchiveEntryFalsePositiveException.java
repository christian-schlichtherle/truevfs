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
 * positive archive file which actually exists as a plain file or directory
 * in an enclosing archive file.
 * <p>
 * Instances of this class are always associated with an
 * {@code IOException} as their cause.
 */
public abstract class ArchiveEntryFalsePositiveException
extends FalsePositiveException {

    private static final long serialVersionUID = 1234562841928746533L;

    private final ArchiveController enclController;
    private final String enclEntryName;

    ArchiveEntryFalsePositiveException(Archive archive, ArchiveController enclController, String enclEntryName, IOException cause) {
        super(archive, cause);
        assert enclController != archive;
        assert isEnclosedBy(archive, enclController);
        assert enclEntryName != null;
        this.enclController = enclController;
        this.enclEntryName = enclEntryName;
    }

    private static boolean isEnclosedBy(Archive archive, Archive wannabe) {
        assert wannabe != null;
        if (archive.getEnclArchive() == wannabe) {
            return true;
        }
        if (archive.getEnclArchive() == null) {
            return false;
        }
        return isEnclosedBy(archive.getEnclArchive(), wannabe);
    }

    /**
     * Returns the controller which's target file contains the
     * false positive archive file as an archive entry.
     * Never {@code null}.
     * <p>
     * Note that this is not the same
     */
    final ArchiveController getEnclController() {
        return enclController;
    }

    /**
     * Returns the entry name of the false positive archive file.
     * Never {@code null}.
     */
    final String getEnclEntryName() {
        return enclEntryName;
    }
}
