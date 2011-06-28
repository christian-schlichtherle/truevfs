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

import de.schlichtherle.truezip.util.Pool.Releasable;
import de.schlichtherle.truezip.socket.IOEntry;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.fs.FsOutputOption;
import static de.schlichtherle.truezip.fs.FsOutputOptions.*;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.fs.FsEntry;
import de.schlichtherle.truezip.fs.FsEntryName;
import static de.schlichtherle.truezip.fs.FsEntryName.*;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.io.File;
import static java.io.File.*;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import net.jcip.annotations.Immutable;

import static de.schlichtherle.truezip.entry.Entry.Access.*;

/**
 * Adapts a {@link File} instance to a {@link FsEntry}.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@DefaultAnnotation(NonNull.class)
@edu.umd.cs.findbugs.annotations.SuppressWarnings("JCIP_FIELD_ISNT_FINAL_IN_IMMUTABLE_CLASS")
class FileEntry
extends FsEntry
implements IOEntry<FileEntry>, Releasable<IOException> {

    private final File file;
    private final String name;
    volatile @CheckForNull TempFilePool pool;

    FileEntry(final File file) {
        assert null != file;
        this.file = file;
        this.name = file.toString(); // deliberately breaks contract for FsEntry.getName()
    }

    FileEntry(final File file, final FsEntryName name) {
        assert null != file;
        this.file = new File(file, name.getPath());
        this.name = name.toString();
    }

    public final FileEntry createTempFile() throws IOException {
        TempFilePool pool = this.pool;
        if (null == pool)
            pool = this.pool = new TempFilePool(file.getParentFile());
        return pool.allocate();
    }

    @Override
    public void release() throws IOException {
    }

    /** Returns the decorated file. */
    final File getFile() {
        return file;
    }

    @Override
    public final String getName() {
        return name.replace(separatorChar, SEPARATOR_CHAR); // postfix
    }

    @Override
    @SuppressWarnings("unchecked")
    public final Set<Type> getTypes() {
        if (file.isFile())
            return FILE_TYPE_SET;
        else if (file.isDirectory())
            return DIRECTORY_TYPE_SET;
        else if (file.exists())
            return SPECIAL_TYPE_SET;
        else
            return Collections.EMPTY_SET;
    }

    @Override
    public final boolean isType(final Type type) {
        switch (type) {
        case FILE:
            return file.isFile();
        case DIRECTORY:
            return file.isDirectory();
        case SPECIAL:
            return file.exists() && !file.isFile() && !file.isDirectory();
        default:
            return false;
        }
    }

    @Override
    public final long getSize(final Size type) {
        return file.exists() ? file.length() : UNKNOWN;
    }

    @Override
    public final long getTime(Access type) {
        return WRITE == type && file.exists() ? file.lastModified() : UNKNOWN;
    }

    @Override
    public final @Nullable Set<String> getMembers() {
        final String[] list = file.list();
        return null == list
                ? null
                : new HashSet<String>(Arrays.asList(list));
    }

    @Override
    public final InputSocket<FileEntry> getInputSocket() {
        return new FileInputSocket(this);
    }

    @Override
    public final OutputSocket<FileEntry> getOutputSocket() {
        return new FileOutputSocket(NO_OUTPUT_OPTION, null, this);
    }

    final OutputSocket<FileEntry> getOutputSocket(
            BitField<FsOutputOption> options,
            @CheckForNull de.schlichtherle.truezip.entry.Entry template) {
        return new FileOutputSocket(options, template, this);
    }
}
