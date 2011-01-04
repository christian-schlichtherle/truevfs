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
package de.schlichtherle.truezip.io.filesystem.file;

import de.schlichtherle.truezip.io.entry.Entry;
import de.schlichtherle.truezip.io.DecoratorOutputStream;
import de.schlichtherle.truezip.io.filesystem.FSOutputOption;
import de.schlichtherle.truezip.io.socket.IOPool;
import de.schlichtherle.truezip.io.socket.IOSocket;
import de.schlichtherle.truezip.io.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static de.schlichtherle.truezip.io.filesystem.FSOutputOption.APPEND;
import static de.schlichtherle.truezip.io.filesystem.FSOutputOption.CACHE;
import static de.schlichtherle.truezip.io.filesystem.FSOutputOption.CREATE_PARENTS;
import static de.schlichtherle.truezip.io.entry.Entry.Access.WRITE;
import static de.schlichtherle.truezip.io.entry.Entry.UNKNOWN;

/**
 * @see     FSFileInputSocket
 * @author  Christian Schlichtherle
 * @version $Id$
 */
final class FSFileOutputSocket extends OutputSocket<FSFileEntry> {

    private static final String TEMP_FILE_POOL_PREFIX = ".tzp";

    private final FSFileEntry entry;
    private final BitField<FSOutputOption> options;
    private final Entry template;
    private volatile FSTempFilePool pool;

    FSFileOutputSocket(   final @NonNull FSFileEntry entry,
                        final @NonNull BitField<FSOutputOption> options,
                        final @Nullable Entry template) {
        assert null != entry;
        assert null != options;
        this.entry = entry;
        this.options = options;
        this.template = template;
    }

    private FSTempFilePool getTempFilePool() {
        if (null == pool)
            pool = new FSTempFilePool(    TEMP_FILE_POOL_PREFIX, null,
                                        entry.getFile().getParentFile());
        return pool;
    }

    @Override
    public FSFileEntry getLocalTarget() {
        return entry;
    }

    @Override
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
    public OutputStream newOutputStream() throws IOException {
        final File entryFile = entry.getFile();
        if (options.get(CREATE_PARENTS))
            entryFile.getParentFile().mkdirs();
        final FSFileEntry temp = options.get(CACHE) && !entryFile.createNewFile()
                ? getTempFilePool().allocate()
                : entry;
        final File tempFile = temp.getFile();

        class OutputStream extends DecoratorOutputStream {
            boolean closed;

            OutputStream() throws FileNotFoundException {
                super(new FileOutputStream(tempFile, options.get(APPEND))); // Do NOT extend FileIn|OutputStream: They implement finalize(), which may cause deadlocks!
            }

            @Override
            public void close() throws IOException {
                if (closed)
                    return;
                closed = true;
                try {
                    super.close();
                } finally {
                    try {
                        if (temp != entry) {
                            IOException cause = null;
                            try {
                                if (!tempFile.renameTo(entryFile)
                                        && (!entryFile.delete() || !tempFile.renameTo(entryFile)))
                                    IOSocket.copy(  temp.getInputSocket(),
                                                    entry.getOutputSocket());
                            } catch (IOException ex) {
                                throw cause = ex;
                            } finally {
                                try {
                                    ((IOPool.Entry<FSFileEntry>) temp).release();
                                } catch (IOException ex) {
                                    throw (IOException) ex.initCause(cause);
                                }
                            }
                        }
                    } finally {
                        if (null != template) {
                            final long time = template.getTime(WRITE);
                            if (UNKNOWN != time
                                    && !entryFile.setLastModified(time))
                                throw new IOException(entryFile.getPath() + " (cannot preserve last modification time)");
                        }
                    }
                }
            }
        } // class OutputStream

        try {
            if (temp != entry && options.get(APPEND))
                IOSocket.copy(  entry.getInputSocket(),
                                temp.getOutputSocket());
            return new OutputStream();
        } catch (IOException cause) {
            if (temp != entry) {
                try {
                    ((IOPool.Entry<FSFileEntry>) temp).release();
                } catch (IOException ex) {
                    throw (IOException) ex.initCause(cause);
                }
            }
            throw cause;
        }
    }
}
