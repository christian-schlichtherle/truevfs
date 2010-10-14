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
package de.schlichtherle.truezip.io.socket;

import java.io.IOException;

/**
 * A pooling strategy for common entries.
 *
 * @see     BufferingInputSocket
 * @see     BufferingOutputSocket
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public interface CommonEntryPool<CE extends CommonEntry> {

    /**
     * Allocates a common entry from this pool.
     *
     * @return A non-{@code null} common entry.
     */
    public CE allocate() throws IOException;

    /**
     * Releases a previously allocated common entry to this pool.
     *
     * @param  entry a non-{@code null} common entry.
     * @throws IllegalArgumentException if the given entry is not allocated
     *         by this pool.
     */
    public void release(CE entry) throws IOException;
}
