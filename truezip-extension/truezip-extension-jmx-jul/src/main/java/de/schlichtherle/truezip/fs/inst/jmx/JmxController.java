/*
 * Copyright 2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.fs.inst.jmx;

import de.schlichtherle.truezip.fs.FsController;
import de.schlichtherle.truezip.fs.FsModel;
import de.schlichtherle.truezip.fs.inst.InstrumentingController;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
abstract class JmxController<M extends FsModel>
extends InstrumentingController<M> {

    JmxController(
            FsController<? extends M> controller,
            JmxDirector director) {
        super(controller, director);
    }

    abstract JmxIOStatistics getIOStatistics();
}
