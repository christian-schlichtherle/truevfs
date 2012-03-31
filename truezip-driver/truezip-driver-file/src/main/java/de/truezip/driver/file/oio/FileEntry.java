/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.file.oio;

import de.truezip.kernel.cio.Entry;
import static de.truezip.kernel.cio.Entry.Access.WRITE;
import de.truezip.kernel.cio.IOBuffer;
import de.truezip.kernel.cio.InputSocket;
import de.truezip.kernel.cio.OutputSocket;
import de.truezip.kernel.fs.FsEntry;
import de.truezip.kernel.addr.FsEntryName;
import static de.truezip.kernel.addr.FsEntryName.SEPARATOR_CHAR;
import de.truezip.kernel.option.AccessOption;
import de.truezip.kernel.option.AccessOptions;
import de.truezip.kernel.util.BitField;
import java.io.File;
import static java.io.File.separatorChar;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * Adapts a {@link File} instance to a {@link FsEntry}.
 *
 * @author Christian Schlichtherle
 */
@Immutable
class FileEntry extends FsEntry implements IOBuffer<FileEntry> {

    private static final File CURRENT_DIRECTORY = new File(".");

    private final File file;
    private final String name;

    @edu.umd.cs.findbugs.annotations.SuppressWarnings("JCIP_FIELD_ISNT_FINAL_IN_IMMUTABLE_CLASS")
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

    final FileEntry createTempFile() throws IOException {
        TempFilePool pool = this.pool;
        if (null == pool)
            this.pool = pool = new TempFilePool(getParent(), getFileName());
        return pool.allocate();
    }

    private File getParent() {
        final File file= this.file.getParentFile();
        return null != file ? file : CURRENT_DIRECTORY;
    }

    private String getFileName() {
        // See http://java.net/jira/browse/TRUEZIP-152
        return this.file.getName();
    }

    /**
     * @throws UnsupportedOperationException always
     */
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
    public final Set<Type> getTypes() {
        if (file.isFile())
            return FILE_TYPE_SET;
        else if (file.isDirectory())
            return DIRECTORY_TYPE_SET;
        else if (file.exists())
            return SPECIAL_TYPE_SET;
        else
            return Collections.emptySet();
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
        return null == list ? null : new HashSet<String>(Arrays.asList(list));
    }

    @Override
    public final InputSocket<FileEntry> getInputSocket() {
        return new FileInputSocket(this);
    }

    @Override
    public final OutputSocket<FileEntry> getOutputSocket() {
        return new FileOutputSocket(this, AccessOptions.NONE, null);
    }

    final OutputSocket<FileEntry> getOutputSocket(
            BitField<AccessOption> options,
            @CheckForNull Entry template) {
        return new FileOutputSocket(this, options, template);
    }
}
