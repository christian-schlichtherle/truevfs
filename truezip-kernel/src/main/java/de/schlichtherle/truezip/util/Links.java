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

package de.schlichtherle.truezip.util;

import de.schlichtherle.truezip.util.Link.Type;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import net.jcip.annotations.ThreadSafe;

/**
 * Provides static utility methods for links.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public class Links {

    private Links() {
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
    public static @CheckForNull <T> Link<T> newLink(@CheckForNull T target) {
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
    public static @CheckForNull <T> Link<T> newLink(@NonNull Type type,
                                                    @CheckForNull T target) {
        return null == target ? null : type.newLink(target);
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
    public static @CheckForNull <T> T getTarget(@CheckForNull Link<T> link) {
        return null == link ? null : link.getTarget();
    }
}
