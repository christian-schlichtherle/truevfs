/*
 * Copyright (C) 2007-2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.fs.archive;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.entry.Entry.Type;
import de.schlichtherle.truezip.fs.FsConcurrentModel;
import de.schlichtherle.truezip.socket.IOPool;
import de.schlichtherle.truezip.socket.InputShop;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.socket.ByteArrayIOPool;
import de.schlichtherle.truezip.socket.OutputShop;
import de.schlichtherle.truezip.socket.OutputSocket;
import java.io.CharConversionException;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class DummyArchiveDriver extends FsCharsetArchiveDriver<FsArchiveEntry> {

    private static final IOPool<?> pool = new ByteArrayIOPool();
    private static final Charset charset = Charset.forName("UTF-8");

    public DummyArchiveDriver() {
        super(charset);
    }

    @Override
    public IOPool<?> getPool() {
        return pool;
    }
    
    @Override
    public InputShop<FsArchiveEntry> newInputShop(
            FsConcurrentModel model,
            InputSocket<?> input)
    throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public OutputShop<FsArchiveEntry> newOutputShop(
            FsConcurrentModel model,
            OutputSocket<?> output,
            InputShop<FsArchiveEntry> source)
    throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public FsArchiveEntry newEntry(String name, Type type, Entry template)
    throws CharConversionException {
        return new DummyArchiveEntry(name, type, template);
    }
}
