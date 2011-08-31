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
package de.schlichtherle.truezip.fs;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import net.jcip.annotations.ThreadSafe;

/**
 * An abstract decorator for a file system model.
 *
 * @param   <M> The type of the decorated file system model.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@DefaultAnnotation(NonNull.class)
public abstract class FsDecoratingModel<M extends FsModel> extends FsModel {

    protected final M delegate;

    protected FsDecoratingModel(final M model) {
        if (null == model)
            throw new NullPointerException();
        this.delegate = model;
    }

    @Override
    public FsMountPoint getMountPoint() {
        return delegate.getMountPoint();
    }

    @Override
    public FsModel getParent() {
        return delegate.getParent();
    }

    @Override
    public boolean isTouched() {
        return delegate.isTouched();
    }

    @Override
    public void setTouched(boolean touched) {
        delegate.setTouched(touched);
    }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return new StringBuilder()
                .append(getClass().getName())
                .append("[delegate=")
                .append(delegate)
                .append("]")
                .toString();
    }
}
