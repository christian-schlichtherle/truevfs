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
package de.schlichtherle.truezip.util;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public interface Pointer<T> {
    T get();

    Type getType();

    interface Factory {
        <T> Pointer<T> newPointer(T object);
    }

    enum Type implements Factory {
        WEAK {
            @Override
            public <T> Pointer<T> newPointer(T referent) {
                return new Weak<T>(referent);
            }
        },

        SOFT {
            @Override
            public <T> Pointer<T> newPointer(T referent) {
                return new Soft<T>(referent);
            }
        },

        STRONG {
            @Override
            public <T> Pointer<T> newPointer(T referent) {
                return new Strong<T>(referent);
            }
        };
    }

    class Strong<T> implements Pointer<T> {
        private final T target;

        public Strong(final T target) {
            this.target = target;
        }

        @Override
        public T get() {
            return target;
        }

        @Override
        public Type getType() {
            return Type.STRONG;
        }
    }

    class Soft<T> extends SoftReference<T> implements Pointer<T> {
        public Soft(T target) {
            super(target);
        }

        @Override
        public Type getType() {
            return Type.SOFT;
        }
    }

    class Weak<T> extends WeakReference<T> implements Pointer<T> {
        public Weak(T target) {
            super(target);
        }

        @Override
        public Type getType() {
            return Type.WEAK;
        }
    }
}
