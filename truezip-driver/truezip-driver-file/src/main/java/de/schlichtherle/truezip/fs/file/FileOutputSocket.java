/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.file;

import de.schlichtherle.truezip.entry.Entry;
import static de.schlichtherle.truezip.entry.Entry.Access.WRITE;
import static de.schlichtherle.truezip.entry.Entry.UNKNOWN;
import de.schlichtherle.truezip.fs.FsOutputOption;
import static de.schlichtherle.truezip.fs.FsOutputOption.*;
import de.schlichtherle.truezip.socket.IOSocket;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.*;
import static java.lang.Boolean.TRUE;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * An output socket for a file entry.
 *
 * @see     FileInputSocket
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
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
            // This is obviously NOT atomic.
            if (TRUE.equals(exists)
                    || null == exists && (exists = entryFile.exists())) {
                if (!entryFile.canWrite())
                    throw new FileNotFoundException(entryFile + " (cannot write)");
            } else {
                if (!entryFile.createNewFile())
                    throw new FileNotFoundException(entryFile + " (already exists)");
            }
            temp = entry.createTempFile();
        } else {
            temp = entry;
        }
        if (options.get(CREATE_PARENTS) && !TRUE.equals(exists)) {
            final File parentFile = entryFile.getParentFile();
            if (null != parentFile)
                if (!parentFile.mkdirs() && !parentFile.isDirectory())
                    throw new IOException(parentFile + " (cannot create directories)");
        }
        return temp;
    }

    private void append(final FileEntry temp) throws IOException {
        if (temp != entry && options.get(APPEND) && entry.getFile().exists())
            IOSocket.copy(entry.getInputSocket(), temp.getOutputSocket());
    }

    private void close(final FileEntry temp, final boolean commit)
    throws IOException {
        final File entryFile = entry.getFile();
        if (temp != entry) {
            final File tempFile = temp.getFile();
            copyAttributes(tempFile);
            if (commit) {
                if (!move(tempFile, entryFile)) {
                    IOSocket.copy(  temp.getInputSocket(),
                                    entry.getOutputSocket());
                    copyAttributes(entryFile);
                }
                release(temp, null);
            } else {
                // Leave temp file for post-mortem analysis.
            }
        } else {
            copyAttributes(entryFile);
        }
    }

    private void copyAttributes(final File file) throws IOException {
        final Entry template = this.template;
        if (null == template)
            return;
        final long time = template.getTime(WRITE);
        if (UNKNOWN != time && !file.setLastModified(time))
            throw new IOException(file + " (cannot preserve last modification time)");
    }

    private static boolean move(File src, File dst) {
        return src.exists()
                && (!dst.exists() || dst.delete())
                && src.renameTo(dst);
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

    @Override
    @SuppressWarnings("deprecation")
    public OutputStream newOutputStream() throws IOException {
        final FileEntry temp = begin();

        class OutputStream extends de.schlichtherle.truezip.io.IOExceptionOutputStream {
            boolean closed;

            @CreatesObligation
            @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
            OutputStream() throws FileNotFoundException {
                super(new FileOutputStream(temp.getFile(), options.get(APPEND))); // Do NOT extend FileOutputStream: It implements finalize(), which may cause deadlocks!
            }

            @Override
            public void close() throws IOException {
                if (closed)
                    return;
                super.close();
                closed = true;
                FileOutputSocket.this.close(temp, null == exception);
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
