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

package de.schlichtherle.truezip.util;

/**
 * Provides static utility methods for dealing with links.
 * This class cannot get instantiated outside its package.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class Links {

    Links() {
    }

    /**
     * Returns a nullable link to the given target.
     * The returned link is {@code null} if and only if {@code target}
     * is {@code null}.
     *
     * @param  <T> The type of the target.
     * @param  target the nullable target.
     * @return A nullable link to the given target.
     */
    public static <T> Link<T> ref(final T target) {
        class TargetIOReference implements Link<T> {
            @Override
            public T getTarget() {
                return target;
            }
        } // class TargetIOReference
        return target == null ? null : new TargetIOReference();
    }

    /**
     * Returns the {@link Link#getTarget() target} of the given nullable link.
     * The returned target is {@code null} if and only if either the given
     * link is {@code null} or its target is {@code null}.
     *
     * @param  <T> The type of the target.
     * @param  reference a nullable link to the target.
     * @return The {@link Link#getTarget() target} of the given nullable link.
     */
    public static <T> T deref(final Link<T> reference) {
        return reference == null ? null : reference.getTarget();
    }
}
