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
package de.schlichtherle.truezip.key;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
final class DummyKey implements SafeKey<DummyKey> {

    private static volatile int count;

    private final int key = count++;
    boolean reset;

    @Override
    public DummyKey clone() {
        try {
            return (DummyKey) super.clone();
        } catch (CloneNotSupportedException ex) {
            throw new AssertionError(ex);
        }
    }

    @Override
    public void reset() {
        reset = true;
    }

    @Override
    public boolean equals(Object that) {
        return that instanceof DummyKey && this.key == ((DummyKey) that).key;
    }

    @Override
    public int hashCode() {
        return key;
    }
}
