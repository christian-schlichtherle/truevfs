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

import de.schlichtherle.truezip.io.FilterInputStream;
import de.schlichtherle.truezip.io.rof.ReadOnlyFile;
import de.schlichtherle.truezip.io.rof.SimpleReadOnlyFile;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * @see     BufferingOutputSocket
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public class BufferingInputSocket<CE extends CommonEntry>
extends FilterInputSocket<CE> {

    private final FileCreator creator;

    public BufferingInputSocket(InputSocket<? extends CE> input) {
        this(input, new TempFileCreator());
    }

    public BufferingInputSocket(    final InputSocket<? extends CE> input,
                                    final FileCreator creator) {
        super(input);
        if (null == creator)
            throw new NullPointerException();
        this.creator = creator;
    }

    @SuppressWarnings("ThrowableInitCause")
    private File createTemporaryInputFile() throws IOException {
        final File temp = creator.createFile();
        IOException cause = null;
        boolean ok = false;
        try {
            CommonEntry peer = getPeerTarget();
            if (null == peer)
                peer = new FileEntry(temp);
            try {
                IOSocket.copy(  getInputSocket(),
                                new FileOutputSocket<CommonEntry>(peer, temp));
            } catch (IOException ex) {
                throw cause = ex;
            }
            ok = true;
        } finally {
            if (!ok && !temp.delete())
                throw (IOException) new IOException(temp.getPath() + " (cannot delete temporary input file)").initCause(cause);
        }
        return temp;
    }

    @Override
    public InputStream newInputStream() throws IOException {
        final File temp = createTemporaryInputFile();

        class InputStream extends FilterInputStream {
            boolean closed;

            InputStream() throws FileNotFoundException {
                super(new FileInputStream(temp)); // Do NOT extend FileIn|OutputStream: They implement finalize(), which may cause deadlocks!
            }

            @Override
            @SuppressWarnings("ThrowableInitCause")
            public void close() throws IOException {
                if (closed)
                    return;
                closed = true;
                IOException cause = null;
                try {
                    try {
                        super.close();
                    } catch (IOException ex) {
                        throw cause = ex;
                    }
                } finally {
                    if (!temp.delete())
                        throw (IOException) new IOException(temp.getPath() + " (cannot delete temporary input file)").initCause(cause);
                }
            }
        }

        return new InputStream();
    }

    @Override
    public ReadOnlyFile newReadOnlyFile() throws IOException {
        final File temp = createTemporaryInputFile();

        class ReadOnlyFile extends SimpleReadOnlyFile {
            boolean closed;

            ReadOnlyFile() throws FileNotFoundException {
                super(temp);
            }

            @Override
            @SuppressWarnings("ThrowableInitCause")
            public void close() throws IOException {
                if (closed)
                    return;
                closed = true;
                IOException cause = null;
                try {
                    try {
                        super.close();
                    } catch (IOException ex) {
                        throw cause = ex;
                    }
                } finally {
                    if (!temp.delete())
                        throw (IOException) new IOException(temp.getPath() + " (cannot delete temporary input file)").initCause(cause);
                }
            }
        }

        return new ReadOnlyFile();
    }
}
