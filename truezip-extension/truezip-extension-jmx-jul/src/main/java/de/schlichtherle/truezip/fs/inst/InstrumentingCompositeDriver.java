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
package de.schlichtherle.truezip.fs.inst;

import de.schlichtherle.truezip.fs.FsCompositeDriver;
import de.schlichtherle.truezip.fs.FsController;
import de.schlichtherle.truezip.fs.FsModel;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import net.jcip.annotations.Immutable;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@DefaultAnnotation(NonNull.class)
public class InstrumentingCompositeDriver implements FsCompositeDriver {

    protected final FsCompositeDriver delegate;
    protected final InstrumentingDirector director;

    public InstrumentingCompositeDriver(final FsCompositeDriver driver, final InstrumentingDirector director) {
        if (null == driver)
            throw new NullPointerException();
        this.director = director.check();
        this.delegate = driver;
    }

    @Override
    public FsController<?> newController(FsModel model, FsController<?> parent) {
        return director.instrument(delegate.newController(director.instrument(model, this), parent), this);
    }
}
