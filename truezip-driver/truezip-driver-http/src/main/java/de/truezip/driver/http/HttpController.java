/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.http;

import de.truezip.kernel.*;
import de.truezip.kernel.cio.Entry;
import de.truezip.kernel.cio.Entry.Access;
import static de.truezip.kernel.cio.Entry.Access.READ;
import de.truezip.kernel.cio.Entry.Type;
import static de.truezip.kernel.cio.Entry.Type.FILE;
import de.truezip.kernel.cio.IOPool;
import de.truezip.kernel.cio.InputSocket;
import de.truezip.kernel.cio.OutputSocket;
import de.truezip.kernel.util.BitField;
import java.io.IOException;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;
import org.apache.http.HttpResponse;

/**
 * A file system controller for the HTTP(S) schemes.
 * 
 * @author  Christian Schlichtherle
 */
@Immutable
public class HttpController extends FsAbstractController<FsModel>  {

    private static final BitField<Access> READ_ONLY = BitField.of(READ);

    private final HttpDriver driver;

    HttpController(final HttpDriver driver, final FsModel model) {
        super(model);
        if (null != model.getParent())
            throw new IllegalArgumentException();
        assert null != driver;
        this.driver = driver;
    }

    final IOPool<?> getPool() {
        return driver.getPool();
    }

    final HttpResponse executeHead(HttpEntry entry) throws IOException {
        return driver.executeHead(entry);
    }

    final HttpResponse executeGet(HttpEntry entry) throws IOException {
        return driver.executeGet(entry);
    }

    protected HttpEntry newEntry(FsEntryName name) {
        return new HttpEntry(this, name);
    }

    final FsPath resolve(FsEntryName name) {
        return getMountPoint().resolve(name);
    }

    @Override
    public FsController<?> getParent() {
        return null;
    }

    @Override
    public boolean isReadOnly() throws IOException {
        return false;
    }

    @Override
    public HttpEntry stat(
            final BitField<FsAccessOption> options, final FsEntryName name)
    throws IOException {
        HttpEntry entry = newEntry(name);
        return entry.isType(FILE) ? entry : null;
    }

    @Override
    public void checkAccess(
            final BitField<FsAccessOption> options, final FsEntryName name, final BitField<Access> types)
    throws IOException {
        if (!types.isEmpty() && !READ_ONLY.equals(types))
            throw new FsReadOnlyFileSystemException();
        executeHead(newEntry(name));
    }

    @Override
    public void setReadOnly(FsEntryName name) throws IOException {
    }

    @Override
    public boolean setTime(
            BitField<FsAccessOption> options, FsEntryName name, BitField<Access> types, long value)
    throws IOException {
        throw new FsReadOnlyFileSystemException();
    }

    @Override
    public InputSocket<?> input(
            BitField<FsAccessOption> options, FsEntryName name) {
        return newEntry(name).newInputSocket(options);
    }

    @Override
    public OutputSocket<?> output(
            BitField<FsAccessOption> options, FsEntryName name, @CheckForNull
    Entry template) {
        return newEntry(name).newOutputSocket(options, template);
    }

    @Override
    public void mknod(  final BitField<FsAccessOption> options, final FsEntryName name, final Type type, @CheckForNull
    final Entry template)
    throws IOException {
        throw new FsReadOnlyFileSystemException();
    }

    @Override
    public void unlink(BitField<FsAccessOption> options, FsEntryName name)
    throws IOException {
        throw new FsReadOnlyFileSystemException();
    }

    @Override
    public void sync(BitField<FsSyncOption> options) {
    }
}
