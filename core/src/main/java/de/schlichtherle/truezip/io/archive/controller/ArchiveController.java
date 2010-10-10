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

import static de.schlichtherle.truezip.io.archive.entry.ArchiveEntry.SEPARATOR_CHAR;
import static de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystems.isRoot;
import static de.schlichtherle.truezip.io.Paths.cutTrailingSeparators;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
abstract class ArchiveController extends FileSystemController {

    private final ArchiveModel model;
    private final FileSystemController enclController;
    private final String enclPath;

    /**
     * Constructs a new archive controller.
     *
     * @param model the non-{@code null} archive model.
     * @throws NullPointerExecption if {@code model} is {@code null} or if
     *         looking up the enclosing file system controller fails.
     */
    ArchiveController(final ArchiveModel model) {
        this.model = model;
        this.enclController = FileSystemControllers
                .getController(getModel().getEnclModel().getMountPoint());
        this.enclPath = model
                .getEnclModel()
                .getMountPoint()
                .relativize(model.getMountPoint())
                .getPath();
    }

    @Override
    protected final ArchiveModel getModel() {
        return model;
    }

    /** Returns the file system controller for the enclosing file system. */
    protected FileSystemController getEnclController() {
        return enclController;
    }

    /**
     * Resolves the given relative {@code path} against the relative path of
     * this controller's archive file within its enclosing file system.
     */
    protected String getEnclPath(final String path) {
        return isRoot(path)
                ? cutTrailingSeparators(enclPath, SEPARATOR_CHAR)
                : enclPath + path;
    }

    @Override
    public final boolean isTouched() {
        return getModel().isTouched();
    }
}
