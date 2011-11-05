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
package de.schlichtherle.truezip.fs.inst.comp;

import de.schlichtherle.truezip.fs.FsDefaultManager;
import de.schlichtherle.truezip.fs.FsFailSafeManager;
import de.schlichtherle.truezip.fs.FsManager;
import de.schlichtherle.truezip.fs.inst.jmx.JmxDirector;
import de.schlichtherle.truezip.fs.inst.jul.JulDirector;
import de.schlichtherle.truezip.fs.spi.FsManagerService;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import net.jcip.annotations.Immutable;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@DefaultAnnotation(NonNull.class)
public final class CompositeManagerService extends FsManagerService {

    private static final FsManager manager =
            JmxDirector.SINGLETON.instrument(
                JulDirector.SINGLETON.instrument(
                    new FsFailSafeManager(
                        new FsDefaultManager())));

    @Override
    public FsManager get() {
        return manager;
    }

    /**
     * Returns 100.
     * 
     * @return 100.
     */
    @Override
    public int getPriority() {
        return 100;
    }
}
