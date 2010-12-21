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
import de.schlichtherle.truezip.io.DecoratingOutputStream;
import de.schlichtherle.truezip.io.socket.IOSocket;
import de.schlichtherle.truezip.io.filesystem.OutputOption;
import de.schlichtherle.truezip.io.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static de.schlichtherle.truezip.io.filesystem.OutputOption.APPEND;
import static de.schlichtherle.truezip.io.filesystem.OutputOption.CACHE;
import static de.schlichtherle.truezip.io.filesystem.OutputOption.CREATE_PARENTS;
import static de.schlichtherle.truezip.io.entry.Entry.Access.WRITE;
import static de.schlichtherle.truezip.io.entry.Entry.UNKNOWN;

/**
 * @see     FileInputSocket
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public final class FileOutputSocket extends OutputSocket<FileEntry> {

    private static final BitField<OutputOption> NO_OUTPUT_OPTIONS
            = BitField.noneOf(OutputOption.class);
    private static final String TEMP_FILE_POOL_PREFIX = ".tzp";

    private final FileEntry file;
    private final Entry template;
    private final BitField<OutputOption> options;
    private volatile TempFilePool pool;

    public static OutputSocket<FileEntry> get(FileEntry file) {
        return new FileOutputSocket(file, NO_OUTPUT_OPTIONS, null);
    }

    public static OutputSocket<FileEntry> get(
            FileEntry file,
            BitField<OutputOption> options,
            Entry template) {
        return new FileOutputSocket(file, options, template);
    }

    private FileOutputSocket(   final FileEntry file,
                                final BitField<OutputOption> options,
                                final Entry template) {
        if (null == file || null == options)
            throw new NullPointerException();
        this.file = file;
        this.template = template;
        this.options = options;
    }

    private TempFilePool getTempFilePool() {
        if (null == pool)
            pool = new TempFilePool(    TEMP_FILE_POOL_PREFIX, null,
                                        file.getFile().getParentFile());
        return pool;
    }

    @Override
    public FileEntry getLocalTarget() {
        return file;
    }

    @Override
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
    public OutputStream newOutputStream() throws IOException {
        final File fileTarget = file.getFile();
        if (options.get(CREATE_PARENTS))
            fileTarget.getParentFile().mkdirs();
        final FileEntry temp = options.get(CACHE) && !fileTarget.createNewFile()
                ? getTempFilePool().allocate()
                : file;
        final File tempTarget = temp.getFile();

        class OutputStream extends DecoratingOutputStream {
            boolean closed;

            OutputStream() throws FileNotFoundException {
                super(new FileOutputStream(tempTarget, options.get(APPEND))); // Do NOT extend FileIn|OutputStream: They implement finalize(), which may cause deadlocks!
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
                        if (temp != file) {
                            IOException cause = null;
                            try {
                                if (!tempTarget.renameTo(fileTarget)
                                        && (!fileTarget.delete() || !tempTarget.renameTo(fileTarget)))
                                    IOSocket.copy(  FileInputSocket.get(temp),
                                                    FileOutputSocket.get(file));
                            } catch (IOException ex) {
                                throw cause = ex;
                            } finally {
                                try {
                                    getTempFilePool().release(temp);
                                } catch (IOException ex) {
                                    throw (IOException) ex.initCause(cause);
                                }
                            }
                        }
                    } finally {
                        if (null != template) {
                            final long time = template.getTime(WRITE);
                            if (UNKNOWN != time
                                    && !fileTarget.setLastModified(time))
                                throw new IOException(fileTarget.getPath() + " (cannot preserve last modification time)");
                        }
                    }
                }
            }
        } // class OutputStream

        try {
            if (temp != file && options.get(APPEND))
                IOSocket.copy(  FileInputSocket.get(file),
                                FileOutputSocket.get(temp));
            return new OutputStream();
        } catch (IOException cause) {
            if (temp != file) {
                try {
                    getTempFilePool().release(temp);
                } catch (IOException ex) {
                    throw (IOException) ex.initCause(cause);
                }
            }
            throw cause;
        }
    }
}
