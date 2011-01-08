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
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import net.jcip.annotations.ThreadSafe;

/**
 * Filters the list of federated file systems managed by the decorated file
 * system manager so that their mount point starts with the prefix provided
 * to the {@link #FSFilterManager1 constructor}.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public final class FSFilterManager1
extends FSDecoratorManager1<FSManager1> {

    private final FSMountPoint1 prefix;

    /**
     * Constructs a new prefix filter file system manager from the given file
     * system manager and mount point prefix.
     *
     * @param manager the decorated file system manager.
     * @param prefix the prefix of the mount point used to filter all federated
     *        file systems of the decorated file system manager.
     */
    public FSFilterManager1(
            @NonNull final FSManager1 manager,
            @NonNull final FSMountPoint1 prefix) {
        super(manager);
        this.prefix = prefix.hierarchicalize();
    }

    @Override
    public int getSize() {
        return getControllers().size();
    }

    @Override
    public Iterator<FsController<?>> iterator() {
        return getControllers().iterator();
    }

    private Collection<FsController<?>> getControllers() {
        final List<FsController<?>> snapshot
                = new ArrayList<FsController<?>>(
                    (int) (delegate.getSize() / .75f) + 1);
        for (FsController<?> controller : delegate) {
            final FSMountPoint1 mountPoint
                    = controller.getModel().getMountPoint().hierarchicalize();
            if (mountPoint.getScheme().equals(prefix.getScheme())
                    && mountPoint.getUri().getPath().startsWith(
                        prefix.getUri().getPath()))
                snapshot.add(controller);
        }
        return snapshot;
    }
}
