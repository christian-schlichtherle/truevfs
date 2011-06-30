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
package de.schlichtherle.truezip.fs.http;

import de.schlichtherle.truezip.io.Streams;
import de.schlichtherle.truezip.rof.DecoratingReadOnlyFile;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.socket.IOPool;
import de.schlichtherle.truezip.socket.InputSocket;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.io.InputStream;
import net.jcip.annotations.ThreadSafe;

/**
 * An input socket for HTTP(S) entries.
 * 
 * @see     HttpOutputSocket
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@DefaultAnnotation(NonNull.class)
final class HttpInputSocket extends InputSocket<HttpEntry> {

    private final HttpEntry entry;

    HttpInputSocket(final HttpEntry entry) {
        assert null != entry;
        this.entry = entry;
    }

    @Override
    public HttpEntry getLocalTarget() {
        return entry;
    }

    @Override
    public ReadOnlyFile newReadOnlyFile() throws IOException {
        final IOPool.Entry<?>
                temp = entry.getController().getDriver().getPool().allocate();
        try {
            Streams.copy(   entry.getConnection().getInputStream(),
                            temp.getOutputSocket().newOutputStream());
        } catch (IOException ex) {
            temp.release();
            throw ex;
        }

        class TempReadOnlyFile extends DecoratingReadOnlyFile {
            boolean closed;

            TempReadOnlyFile() throws IOException {
                super(temp.getInputSocket().newReadOnlyFile()); // bind(*) is considered redundant for IOPool.Entry
            }

            @Override
            public void close() throws IOException {
                if (closed)
                    return;
                closed = true;
                try {
                    super.close();
                } finally {
                    temp.release();
                }
            }
        } // class TempReadOnlyFile

        return new TempReadOnlyFile();
    }

    @Override
    public InputStream newInputStream() throws IOException {
        return entry.getConnection().getInputStream();
    }
}
