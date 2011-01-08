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
package de.schlichtherle.truezip.io.fs;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Iterator;
import net.jcip.annotations.ThreadSafe;

/**
 * @param   <M> The type of the file system model.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public abstract class FSDecoratorManager1<M extends FSManager1>
extends FSManager1 {

    protected final M delegate;
    
    /**
     * Constructs a new decorating file system manager.
     *
     * @param manager the decorated file system manager.
     */
    protected FSDecoratorManager1(@NonNull final M manager) {
        if (null == manager)
            throw new NullPointerException();
        this.delegate = manager;
    }

    @Override
    public FsController<?>
    getController(  FSMountPoint1 mountPoint,
                    FSDriver1 driver) {
        return delegate.getController(mountPoint, driver);
    }

    @Override
    public int getSize() {
        return delegate.getSize();
    }

    @Override
    public Iterator<FsController<?>> iterator() {
        return delegate.iterator();
    }
}
