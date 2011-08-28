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

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
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
@DefaultAnnotation(NonNull.class)
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
     * A factory for links which defines the terms and conditions for clearing
     * their target.
     */
    enum Type {

        /** This reference type never clears the target of a link. */
        STRONG {
            @Override
            <T> Link<T> newLink(T target, ReferenceQueue<? super T> queue) {
                return new Strong<T>(target);
            }
        },

        /**
         * This reference type clears the target of a link according to the
         * terms and conditions for a {@link SoftReference}.
         */
        SOFT {
            @Override
            <T> Link<T> newLink(T target, ReferenceQueue<? super T> queue) {
                return new Soft<T>(target, queue);
            }
        },

        /**
         * This reference type clears the target of a link according to the
         * terms and conditions for a {@link WeakReference}.
         */
        WEAK {
            @Override
            <T> Link<T> newLink(T target, ReferenceQueue<? super T> queue) {
                return new Weak<T>(target, queue);
            }
        },

        /**
         * This reference type clears the target of a link according to the
         * terms and conditions for a {@link PhantomReference}.
         */
        PHANTOM {
            @Override
            public <T> Link<T> newLink(T target, ReferenceQueue<? super T> queue) {
                return new Phantom<T>(target, queue);
            }
        };

        /** Returns a new typed link to the given nullable target. */
        public <T> Link<T> newLink(@CheckForNull T target) {
            return newLink(target, null);
        }

        /** Returns a new typed link to the given nullable target. */
        abstract <T> Link<T> newLink(@CheckForNull T target, @CheckForNull ReferenceQueue<? super T> queue);

        /** A strong reference. */
        private static final class Strong<T> implements Link<T> {
            private final T target;

            Strong(final T target) {
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

        /** Adapts its subclass to the {@link Link} interface. */
        private static final class Soft<T> extends SoftReference<T>
        implements Link<T> {
            Soft(T target, ReferenceQueue<? super T> queue) {
                super(target, queue);
            }

            @Override
            public T getTarget() {
                return super.get();
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

        /** Adapts its subclass to the {@link Link} interface. */
        private static final class Weak<T> extends WeakReference<T>
        implements Link<T> {
            Weak(T target, ReferenceQueue<? super T> queue) {
                super(target, queue);
            }

            @Override
            public T getTarget() {
                return super.get();
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

        /** Adapts its subclass to the {@link Link} interface. */
        private static final class Phantom<T> extends PhantomReference<T>
        implements Link<T> {
            Phantom(T target, ReferenceQueue<? super T> queue) {
                super(target, queue);
            }

            @Override
            public T getTarget() {
                return super.get();
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
