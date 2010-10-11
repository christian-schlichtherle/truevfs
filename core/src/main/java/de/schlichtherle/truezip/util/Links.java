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

import de.schlichtherle.truezip.util.Link.Type;

/**
 * Provides static utility methods for links.
 * This class cannot get instantiated outside its package.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class Links {

    Links() {
    }

    /**
     * Returns a nullable (strong) link to the given target.
     * The returned link is {@code null} if and only if {@code target}
     * is {@code null}.
     *
     * @param  <T> The type of the target.
     * @param  target the nullable target.
     * @return A nullable (strong) link to the given target.
     */
    public static <T> Link<T> newLink(final T target) {
        return newLink(Type.STRONG, target);
    }

    /**
     * Returns a nullable typed link to the given target.
     * The returned typed link is {@code null} if and only if {@code target}
     * is {@code null}.
     *
     * @param  <T> The type of the target.
     * @param  target the nullable target.
     * @return A nullable typed link to the given target.
     */
    public static <T> Link<T> newLink(final Type type, final T target) {
        return target == null ? null : type.newLink(target);
    }

    /**
     * Returns the nullable {@link Link#getTarget() target} of the given link.
     * The returned target is {@code null} if and only if either the given
     * link is {@code null} or its target is {@code null}.
     *
     * @param  <T> The type of the target.
     * @param  link a nullable link.
     * @return The nullable {@link Link#getTarget() target} of the given link.
     */
    public static <T> T getTarget(final Link<T> link) {
        return link == null ? null : link.getTarget();
    }
}
