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
import edu.umd.cs.findbugs.annotations.Nullable;
import net.jcip.annotations.ThreadSafe;

/**
 * Defines the common properties of a file system.
 * <p>
 * Sub-classes must be thread-safe, too.
 *
 * @see     FsController
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@DefaultAnnotation(NonNull.class)
public abstract class FsModel {

    /**
     * Returns the mount point of this file system model.
     * <p>
     * The mount point may be used to construct error messages or to locate
     * and access file system metadata which is stored outside the file system,
     * e.g. in-memory stored passwords for RAES encrypted ZIP files.
     *
     * @return The mount point of this file system model.
     */
    public abstract FsMountPoint getMountPoint();

    /**
     * Returns the model of the parent file system or {@code null} if and
     * only if the file system is not federated, i.e. if it's not a member of
     * a parent file system.
     *
     * @return The nullable parent file system model.
     */
    @Nullable
    public abstract FsModel getParent();

    /**
     * Returns {@code true} if and only if some state associated with the
     * federated file system has been modified so that the
     * corresponding {@link FsController} must not get discarded until
     * the next {@link FsController#sync sync}.
     * 
     * @return {@code true} if and only if some state associated with the
     *         federated file system has been modified so that the
     *         corresponding {@link FsController} must not get discarded until
     *         the next {@link FsController#sync sync}.
     */
    public abstract boolean isTouched();

    /**
     * Sets the value of the property {@code touched}.
     *
     * @param touched the new value of this property.
     */
    public abstract void setTouched(boolean touched);

    /**
     * Two file system models are considered equal if and only if they are
     * identical.
     * This can't get overriden.
     * 
     * @param that the object to compare.
     */
    @Override
    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    public final boolean equals(Object that) {
        return this == that;
    }

    /**
     * Returns a hash code which is consistent with {@link #equals}.
     * This can't get overriden.
     */
    @Override
    public final int hashCode() {
        return super.hashCode();
    }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return new StringBuilder()
                .append(getClass().getName())
                .append("[mountPoint=")
                .append(getMountPoint())
                .append(",parent=")
                .append(getParent())
                .append(",touched=")
                .append(isTouched())
                .append("]")
                .toString();
    }
}
