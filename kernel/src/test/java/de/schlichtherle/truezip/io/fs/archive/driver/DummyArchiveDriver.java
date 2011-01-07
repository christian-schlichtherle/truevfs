/*
 * Copyright (C) 2007-2010 Schlichtherle IT Services
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
package de.schlichtherle.truezip.io.fs.archive.driver;

import de.schlichtherle.truezip.io.fs.archive.ArchiveEntry;
import de.schlichtherle.truezip.io.fs.archive.DummyArchiveEntry;
import de.schlichtherle.truezip.io.entry.Entry;
import de.schlichtherle.truezip.io.entry.Entry.Type;
import de.schlichtherle.truezip.io.fs.concurrency.FSConcurrencyModel;
import de.schlichtherle.truezip.io.socket.InputShop;
import de.schlichtherle.truezip.io.socket.InputSocket;
import de.schlichtherle.truezip.io.socket.OutputShop;
import de.schlichtherle.truezip.io.socket.OutputSocket;
import java.io.CharConversionException;
import java.io.IOException;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public final class DummyArchiveDriver extends ArchiveDriver<ArchiveEntry> {
    private static final long serialVersionUID = 1L;

    @Override
    public InputShop<ArchiveEntry> newInputShop(
            FSConcurrencyModel model,
            InputSocket<?> input)
    throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public OutputShop<ArchiveEntry> newOutputShop(
            FSConcurrencyModel model,
            OutputSocket<?> output,
            InputShop<ArchiveEntry> source)
    throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ArchiveEntry newEntry(String name, Type type, Entry template)
    throws CharConversionException {
        return new DummyArchiveEntry(name, type, template);
    }
}
