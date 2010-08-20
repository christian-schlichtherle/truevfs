/*
 * Copyright (C) 2006-2010 Schlichtherle IT Services
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

package de.schlichtherle.util.concurrent.locks;

/**
 * Similar to {@link java.util.concurrent.locks.ReadWriteLock},
 * but uses the simplified {@link ReentrantLock} interface.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 * @since TrueZIP 6.2
 */
public interface ReadWriteLock {

    /** Returns the lock for reading. */
    ReentrantLock readLock();

    /** Returns the lock for writing. */
    ReentrantLock writeLock();
}
