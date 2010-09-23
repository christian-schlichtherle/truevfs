/*
 * Copyright 2010 Schlichtherle IT Services
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

package de.schlichtherle.truezip.io.socket;

/**
 * Provides static utility methods for dealing with I/O references.
 * This class cannot get instantiated outside its package.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class IOReferences {

    IOReferences() {
    }

    /**
     * Returns a nullable I/O reference to the given target for I/O operations.
     * The returned I/O reference is {@code null} if and only if {@code target}
     * is {@code null}.
     *
     * @param  <T> The type of the target for I/O operations.
     * @param  target the nullable target for I/O operations.
     * @return A nullable I/O reference to the given target for I/O operations.
     */
    public static <T> IOReference<T> ref(final T target) {
        class TargetIOReference implements IOReference<T> {
            @Override
            public T getTarget() {
                return target;
            }
        } // class TargetIOReference
        return target != null ? new TargetIOReference() : null;
    }

    /**
     * Returns the {@link IOReference#getTarget() target} for I/O operations of the
     * given nullable I/O reference.
     * The returned target for I/O operations is {@code null} if and only if
     * either the given reference is {@code null} or its target for I/O
     * operations is {@code null}.
     *
     * @param  <T> The type of the target for I/O operations.
     * @param  reference a nullable I/O reference to the target for I/O
     *         operations.
     * @return The {@link IOReference#getTarget() target} for I/O operations of the
     *         given nullable I/O reference.
     */
    public static <T> T deref(final IOReference<T> reference) {
        return reference != null ? reference.getTarget() : null;
    }
}
