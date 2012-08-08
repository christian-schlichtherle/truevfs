/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.driver.http;

import java.io.IOException;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;
import net.java.truecommons.shed.BitField;
import net.java.truevfs.kernel.spec.*;
import net.java.truevfs.kernel.spec.cio.Entry;
import net.java.truevfs.kernel.spec.cio.Entry.Access;
import static net.java.truevfs.kernel.spec.cio.Entry.Access.READ;
import net.java.truevfs.kernel.spec.cio.Entry.Type;
import static net.java.truevfs.kernel.spec.cio.Entry.Type.FILE;
import net.java.truevfs.kernel.spec.cio.InputSocket;
import net.java.truevfs.kernel.spec.cio.IoBufferPool;
import net.java.truevfs.kernel.spec.cio.OutputSocket;
import org.apache.http.HttpResponse;

/**
 * A file system controller for the HTTP(S) schemes.
 * 
 * @author Christian Schlichtherle
 */
@Immutable
public class HttpController extends FsAbstractController {

    private static final BitField<Access> READ_ONLY = BitField.of(READ);

    private final HttpDriver driver;

    HttpController(final HttpDriver driver, final FsModel model) {
        super(model);
        if (null != model.getParent())
            throw new IllegalArgumentException();
        assert null != driver;
        this.driver = driver;
    }

    final IoBufferPool getPool() {
        return driver.getPool();
    }

    final HttpResponse executeHead(HttpNode entry) throws IOException {
        return driver.executeHead(entry);
    }

    final HttpResponse executeGet(HttpNode entry) throws IOException {
        return driver.executeGet(entry);
    }

    protected HttpNode newEntry(FsNodeName name) {
        return new HttpNode(this, name);
    }

    final FsNodePath resolve(FsNodeName name) {
        return getMountPoint().resolve(name);
    }

    @Override
    public FsController getParent() {
        return null;
    }

    @Override
    public HttpNode stat(
            final BitField<FsAccessOption> options, final FsNodeName name)
    throws IOException {
        HttpNode entry = newEntry(name);
        return entry.isType(FILE) ? entry : null;
    }

    @Override
    public void checkAccess(
            final BitField<FsAccessOption> options, final FsNodeName name, final BitField<Access> types)
    throws IOException {
        if (!types.isEmpty() && !READ_ONLY.equals(types))
            throw new FsReadOnlyFileSystemException();
        executeHead(newEntry(name));
    }

    @Override
    public void setReadOnly(FsNodeName name) throws IOException {
    }

    @Override
    public boolean setTime(
            BitField<FsAccessOption> options, FsNodeName name, BitField<Access> types, long value)
    throws IOException {
        throw new FsReadOnlyFileSystemException();
    }

    @Override
    public InputSocket<?> input(
            BitField<FsAccessOption> options,
            FsNodeName name) {
        return newEntry(name).input(options);
    }

    @Override
    public OutputSocket<?> output(
            BitField<FsAccessOption> options,
            FsNodeName name,
            @CheckForNull Entry template) {
        return newEntry(name).output(options, template);
    }

    @Override
    public void mknod(  final BitField<FsAccessOption> options, final FsNodeName name, final Type type, @CheckForNull
    final Entry template)
    throws IOException {
        throw new FsReadOnlyFileSystemException();
    }

    @Override
    public void unlink(BitField<FsAccessOption> options, FsNodeName name)
    throws IOException {
        throw new FsReadOnlyFileSystemException();
    }

    @Override
    public void sync(BitField<FsSyncOption> options) {
    }
}
