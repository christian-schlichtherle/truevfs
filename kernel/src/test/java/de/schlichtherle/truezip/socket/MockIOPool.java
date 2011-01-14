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
import java.util.HashSet;
import java.util.Set;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public final class MockIOPool implements IOPool<MockIOEntry> {

    final Set<IOPool.Entry<MockIOEntry>> entries = new HashSet<IOPool.Entry<MockIOEntry>>();

    @Override
    public IOPool.Entry<MockIOEntry> allocate() throws IOException {
        final IOPool.Entry<MockIOEntry>
                entry = new MockIOEntry(MockIOEntry.MOCK_ENTRY_NAME) {
            @Override
            public void release() {
                if (!entries.remove(this)) {
                    throw new IllegalArgumentException();
                }
                super.release();
            }
        };
        entries.add(entry);
        return entry;
    }

    @Override
    public void release(IOPool.Entry<MockIOEntry> entry) throws IOException {
        assert false;
        entry.release();
    }
}
