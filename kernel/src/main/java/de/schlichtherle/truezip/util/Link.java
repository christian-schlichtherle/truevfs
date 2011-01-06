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

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;

/**
 * A link has a nullable {@link #getTarget() target} property.
 * This interface is useful if a class is decorating or adapting another class
 * and access to the decorated or adapted object should be provided as part of
 * the public API of the decorating or adapting class.
 *
 * @param   <T> The type of the target.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public interface Link<T> {

    /**
     * Returns the target of this link.
     * <p>
     * The returned object reference may be {@code null} subject to the terms
     * and conditions of sub-interfaces or implementations.
     * 
     * @return The target of this link.
     */
    @Nullable T getTarget();

    /**
     * A link type defines the terms and conditions for clearing its target
     * and is a factory for
     */
    enum Type {

        /** This reference type never clears the target of a link. */
        STRONG {
            @Override
            public <T> Link<T> newLink(T target) {
                return new StrongLink<T>(target);
            }
        },

        /**
         * This reference type clears the target of a link according to the
         * terms and conditions of a {@link SoftReference}.
         */
        SOFT {
            @Override
            public <T> Link<T> newLink(T target) {
                return new SoftLink<T>(target);
            }
        },

        /**
         * This reference type clears the target of a link according to the
         * terms and conditions of a {@link WeakReference}.
         */
        WEAK {
            @Override
            public <T> Link<T> newLink(T target) {
                return new WeakLink<T>(target);
            }
        };

        /** Returns a new typed link to the given nullable target. */
        public abstract @NonNull <T> Link<T> newLink(@Nullable T target);

        private static class StrongLink<T> implements Link<T> {
            private final T target;

            StrongLink(final T target) {
                this.target = target;
            }

            @Override
            public T getTarget() {
                return target;
            }

            @Override
            public String toString() {
                return new StringBuilder()
                        .append(getClass().getName())
                        .append("[target=")
                        .append(getTarget())
                        .append(']')
                        .toString();
            }
        }

        private static class SoftLink<T> extends SoftReference<T>
        implements Link<T> {
            SoftLink(T target) {
                super(target);
            }

            @Override
            public T getTarget() {
                return get();
            }

            @Override
            public String toString() {
                return new StringBuilder()
                        .append(getClass().getName())
                        .append("[target=")
                        .append(getTarget())
                        .append(']')
                        .toString();
            }
        }

        private static class WeakLink<T> extends WeakReference<T>
        implements Link<T> {
            WeakLink(T target) {
                super(target);
            }

            @Override
            public T getTarget() {
                return get();
            }

            @Override
            public String toString() {
                return new StringBuilder()
                        .append(getClass().getName())
                        .append("[target=")
                        .append(getTarget())
                        .append(']')
                        .toString();
            }
        }
    }
}
