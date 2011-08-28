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

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import net.jcip.annotations.ThreadSafe;

/**
 * The default implementation of a file system model.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@DefaultAnnotation(NonNull.class)
public class FsDefaultModel extends FsModel {

    private final FsMountPoint mountPoint;
    private final @CheckForNull FsModel parent;
    private volatile boolean touched;

    public FsDefaultModel(  final FsMountPoint mountPoint,
                            final @CheckForNull FsModel parent) {
        if (!equals(mountPoint.getParent(),
                    (null == parent ? null : parent.getMountPoint())))
            throw new IllegalArgumentException("Parent/Member mismatch!");
        this.mountPoint = mountPoint;
        this.parent = parent;
    }

    private static boolean equals(@CheckForNull Object o1, @CheckForNull Object o2) {
        return o1 == o2 || null != o1 && o1.equals(o2);
    }

    @Override
    public final FsMountPoint getMountPoint() {
        return mountPoint;
    }

    @Override
    public final FsModel getParent() {
        return parent;
    }

    @Override
    public boolean isTouched() {
        return touched;
    }

    @Override
    public void setTouched(boolean touched) {
        this.touched = touched;
    }
}
