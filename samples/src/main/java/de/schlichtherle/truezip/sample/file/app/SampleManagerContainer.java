/*
 * Copyright (C) 2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.sample.file.app;

import de.schlichtherle.truezip.fs.FsFailSafeManager;
import de.schlichtherle.truezip.fs.FsDefaultManager;
import de.schlichtherle.truezip.fs.FsManager;
import de.schlichtherle.truezip.fs.FsManagerService;
import de.schlichtherle.truezip.fs.FsStatisticsManager;

/**
 * A container of a statistics-enabled fail-safe federated file system manager.
 * Note that providing a file system manager container is completely optional:
 * If no file system manager container is provided, the kernel chooses a
 * reasonable default file system manager.
 * This implemetation is just provided for demonstration purposes.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public final class SampleManagerContainer implements FsManagerService {

    static final FsStatisticsManager manager = new FsStatisticsManager(
            new FsFailSafeManager(new FsDefaultManager()));

    @Override
    public FsManager getManager() {
        return manager;
    }
}
