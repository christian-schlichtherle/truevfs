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
package de.schlichtherle.truezip.socket;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import net.jcip.annotations.ThreadSafe;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public final class MockIOPool implements IOPool<MockIOEntry> {

    private static final String MOCK_ENTRY_NAME = "mock";

    final Set<Entry> entries = Collections.synchronizedSet(new HashSet<Entry>());
    volatile int i;

    @Override
    public Entry allocate() throws IOException {
        Entry entry = new Entry();
        entries.add(entry);
        return entry;
    }

    @Override
    public void release(IOPool.Entry<MockIOEntry> entry) throws IOException {
        assert false;
        entry.release();
    }

    public final class Entry extends MockIOEntry implements IOPool.Entry<MockIOEntry> {

        Entry() {
            super(MOCK_ENTRY_NAME + i++);
            setInitialCapacity(4096);
        }

        @Override
        public void release() {
            if (!entries.remove(this))
                throw new IllegalArgumentException();
            setData(null);
        }
    }
}
