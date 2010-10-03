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

import de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystem;
import de.schlichtherle.truezip.io.archive.entry.ArchiveEntry;
import java.io.IOException;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
abstract class FilterArchiveController<AE extends ArchiveEntry>
extends ArchiveController<AE> {

    protected ArchiveController<AE> controller;

    FilterArchiveController(
            ArchiveModel<AE> model,
            ArchiveController<AE> controller) {
        super(model);
        this.controller = controller;
    }

    @Override
    final ArchiveFileSystem<AE> autoMount(boolean autoCreate, boolean createParents)
    throws IOException {
        return controller.autoMount(autoCreate, createParents);
    }
}
