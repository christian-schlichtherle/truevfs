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
package de.schlichtherle.truezip.fs.file;

import de.schlichtherle.truezip.entry.Entry;
import static de.schlichtherle.truezip.entry.Entry.*;
import static de.schlichtherle.truezip.entry.Entry.Access.*;
import de.schlichtherle.truezip.fs.FsOutputOption;
import static de.schlichtherle.truezip.fs.FsOutputOption.*;
import de.schlichtherle.truezip.io.DecoratingOutputStream;
import de.schlichtherle.truezip.socket.IOSocket;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import static java.lang.Boolean.*;

/**
 * An output socket for a file entry.
 *
 * @see     FileInputSocket
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
final class FileOutputSocket extends OutputSocket<FileEntry> {

    private final               FileEntry                entry;
    private final               BitField<FsOutputOption> options;
    private final @CheckForNull Entry                    template;

    FileOutputSocket(   final               FileEntry                entry,
                        final               BitField<FsOutputOption> options,
                        final @CheckForNull Entry                    template) {
        assert null != entry;
        assert null != options;
        if (options.get(EXCLUSIVE) && options.get(APPEND))
            throw new IllegalArgumentException();
        this.entry    = entry;
        this.options  = options;
        this.template = template;
    }

    @Override
    public FileEntry getLocalTarget() {
        return entry;
    }

    private FileEntry begin() throws IOException {
        final FileEntry temp;
        final File entryFile = entry.getFile();
        Boolean exists = null;
        if (options.get(EXCLUSIVE) && (exists = entryFile.exists()))
            throw new IOException(entryFile + " (file exists already)"); // this is obviously not atomic
        if (options.get(CACHE)) {
            if (TRUE.equals(exists)
                    || null == exists && (exists = entryFile.exists()))
                if (!entryFile.canWrite())
                    throw new FileNotFoundException(entryFile + " (cannot write)"); // this is obviously not atomic
            temp = entry.createTempFile();
        } else {
            temp = entry;
        }
        if (!TRUE.equals(exists) && options.get(CREATE_PARENTS))
            entryFile.getParentFile().mkdirs();
        return temp;
    }

    private void commit(final FileEntry temp) throws IOException {
        final File entryFile = entry.getFile();
        final File tempFile = temp.getFile();
        IOException ex = null;
        try {
            if (temp != entry) {
                try {
                    if (!tempFile.renameTo(entryFile)
                            && !(entryFile.delete() && tempFile.renameTo(entryFile)))
                        IOSocket.copy(  temp.getInputSocket(),
                                        entry.getOutputSocket());
                } catch (IOException ex2) {
                    throw ex = ex2;
                } finally {
                    release(temp, ex);
                }
            }
        } finally {
            final Entry template = this.template;
            if (null != template) {
                final long time = template.getTime(WRITE);
                if (UNKNOWN != time && !entryFile.setLastModified(time))
                    throw new IOException(entryFile + " (cannot preserve last modification time)", ex);
            }
        }
    }

    private void release(
            final FileEntry temp,
            final @CheckForNull IOException ex)
    throws IOException {
        try {
            temp.release();
        } catch (IOException ex2) {
            ex2.initCause(ex);
            throw ex2;
        }
    }

    private void append(final FileEntry temp) throws IOException {
        if (temp != entry && options.get(APPEND) && entry.getFile().exists())
            IOSocket.copy(entry.getInputSocket(), temp.getOutputSocket());
    }

    @Override
    public OutputStream newOutputStream() throws IOException {
        final FileEntry temp = begin();

        class OutputStream extends DecoratingOutputStream {
            boolean closed;

            OutputStream() throws FileNotFoundException {
                super(new FileOutputStream(temp.getFile(), options.get(APPEND))); // Do NOT extend FileIn|OutputStream: They implement finalize(), which may cause deadlocks!
            }

            @Override
            public void close() throws IOException {
                if (closed)
                    return;
                closed = true;
                try {
                    delegate.close();
                } finally {
                    commit(temp);
                }
            }
        } // OutputStream

        try {
            append(temp);
            return new OutputStream();
        } catch (IOException ex) {
            release(temp, ex);
            throw ex;
        }
    }
}
