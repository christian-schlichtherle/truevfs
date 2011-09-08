/*
 * Copyright 2011 Schlichtherle IT Services
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
import static de.schlichtherle.truezip.entry.Entry.Type.*;
import de.schlichtherle.truezip.fs.FsController;
import de.schlichtherle.truezip.fs.FsEntryName;
import de.schlichtherle.truezip.fs.FsInputOption;
import de.schlichtherle.truezip.fs.FsModel;
import de.schlichtherle.truezip.fs.FsModelController;
import de.schlichtherle.truezip.fs.FsOutputOption;
import de.schlichtherle.truezip.fs.FsPath;
import de.schlichtherle.truezip.fs.FsSyncException;
import de.schlichtherle.truezip.fs.FsSyncOption;
import de.schlichtherle.truezip.socket.IOPool;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.ExceptionHandler;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.net.URI;
import javax.swing.Icon;
import net.jcip.annotations.Immutable;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;

/**
 * A file system controller for the HTTP(S) schemes.
 * 
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@DefaultAnnotation(NonNull.class)
final class HttpController extends FsModelController<FsModel>  {

    private final HttpDriver driver;

    HttpController(final HttpDriver driver, final FsModel model) {
        super(model);
        if (null != model.getParent())
            throw new IllegalArgumentException();
        assert null != driver;
        this.driver = driver;
    }

    IOPool<?> getPool() {
        return driver.getPool();
    }

    HttpResponse executeHead(URI uri) throws IOException {
        return driver.executeHead(uri);
    }

    HttpResponse executeGet(URI uri) throws IOException {
        return driver.executeGet(uri);
    }

    FsPath resolve(FsEntryName name) {
        return getMountPoint().resolve(name);
    }

    @Override
    public FsController<?> getParent() {
        return null;
    }

    @Override
    public Icon getOpenIcon() throws IOException {
        return null;
    }

    @Override
    public Icon getClosedIcon() throws IOException {
        return null;
    }

    @Override
    public boolean isReadOnly() throws IOException {
        return false;
    }

    @Override
    public HttpEntry getEntry(FsEntryName name) throws IOException {
        HttpEntry entry = new HttpEntry(this, name);
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
        return new HttpEntry(this, name).getInputSocket(options);
    }

    @Override
    public OutputSocket<?> getOutputSocket(
            FsEntryName name,
            BitField<FsOutputOption> options,
            @CheckForNull Entry template) {
        return new HttpEntry(this, name).getOutputSocket(options, template);
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
