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
package de.schlichtherle.truezip.fs;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import net.jcip.annotations.Immutable;

/**
 * An abstract file system controller which implements the {@link #getModel()}
 * method so that it can forward calls to its additional protected methods to
 * this model for the convenience of sub-classes.
 *
 * @since   TrueZIP 7.2
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@DefaultAnnotation(NonNull.class)
public abstract class FsModelController<M extends FsModel>
extends FsController<M>  {

    private final M model;

    /**
     * Constructs a new file system controller for the given model.
     * 
     * @param model the file system model.
     */
    protected FsModelController(final M model) {
        if (null == model)
            throw new NullPointerException();
        this.model = model;
    }

    @Override
    public final M getModel() {
        return model;
    }

    protected FsMountPoint getMountPoint() {
        return model.getMountPoint();
    }

    protected boolean isTouched() {
        return model.isTouched();
    }

    protected void setTouched(boolean touched) {
        model.setTouched(touched);
    }
}
