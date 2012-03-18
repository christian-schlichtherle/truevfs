/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.http;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.entry.Entry.Access;
import de.schlichtherle.truezip.entry.Entry.Type;
import static de.schlichtherle.truezip.entry.Entry.Type.FILE;
import de.schlichtherle.truezip.fs.*;
import de.schlichtherle.truezip.socket.IOPool;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.ExceptionHandler;
import java.io.IOException;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;
import javax.swing.Icon;
import org.apache.http.HttpResponse;

/**
 * A file system controller for the HTTP(S) schemes.
 * 
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
public class HttpController extends FsModelController<FsModel>  {

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
    @Deprecated
    public Icon getOpenIcon() throws IOException {
        return null;
    }

    @Override
    @Deprecated
    public Icon getClosedIcon() throws IOException {
        return null;
    }

    @Override
    public boolean isReadOnly() throws IOException {
        return false;
    }

    @Override
    public HttpEntry getEntry(FsEntryName name) throws IOException {
        HttpEntry entry = newEntry(name);
        return entry.isType(FILE) ? entry : null;
    }

    @Override
    public boolean isReadable(FsEntryName name) throws IOException {
        return null != getEntry(name);
    }

    @Override
    public boolean isWritable(FsEntryName name) throws IOException {
        return false;
    }

    @Override
    public void setReadOnly(FsEntryName name) throws IOException {
    }

    @Override
    public boolean setTime(
            FsEntryName name,
            BitField<Access> types,
            long value,
            BitField<FsOutputOption> options)
    throws IOException {
        throw new ReadOnlyFileSystemTypeException();
    }

    @Override
    public InputSocket<?> getInputSocket(
            FsEntryName name,
            BitField<FsInputOption> options) {
        return newEntry(name).newInputSocket(options);
    }

    @Override
    public OutputSocket<?> getOutputSocket(
            FsEntryName name,
            BitField<FsOutputOption> options,
            @CheckForNull Entry template) {
        return newEntry(name).newOutputSocket(options, template);
    }

    @Override
    public void mknod(  final FsEntryName name,
                        final Type type,
                        final BitField<FsOutputOption> options,
                        final @CheckForNull Entry template)
    throws IOException {
        throw new ReadOnlyFileSystemTypeException();
    }

    @Override
    public void unlink(FsEntryName name, BitField<FsOutputOption> options)
    throws IOException {
        throw new ReadOnlyFileSystemTypeException();
    }

    @Override
    public <X extends IOException>
    void sync(  BitField<FsSyncOption> options,
                ExceptionHandler<? super FsSyncException, X> handler)
    throws X {
    }
}
