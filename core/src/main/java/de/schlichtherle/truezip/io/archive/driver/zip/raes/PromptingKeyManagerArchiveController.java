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
package de.schlichtherle.truezip.io.archive.driver.zip.raes;

import de.schlichtherle.truezip.io.archive.driver.zip.ZipEntry;
import de.schlichtherle.truezip.io.archive.controller.ArchiveController;
import de.schlichtherle.truezip.io.archive.controller.FilterArchiveController;
import de.schlichtherle.truezip.key.PromptingKeyManager;
import java.io.IOException;

import static de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystems.isRoot;

/**
 * This archive controller resets the key provider in the prompting key manager
 * if the target RAES encrypted ZIP archive file gets deleted.
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 */
final class PromptingKeyManagerArchiveController
extends     FilterArchiveController<ZipEntry> {

    /**
     * Constructs a new prompting key manager archive controller.
     *
     * @param controller the non-{@code null} archive controller.
     */
    PromptingKeyManagerArchiveController(
            ArchiveController<? extends ZipEntry> controller) {
        super(controller);
    }

    @Override
    public void unlink(String path) throws IOException {
        getController().unlink(path);
        if (isRoot(path))
            PromptingKeyManager.resetKeyProvider(getModel().getMountPoint());
    }
}
