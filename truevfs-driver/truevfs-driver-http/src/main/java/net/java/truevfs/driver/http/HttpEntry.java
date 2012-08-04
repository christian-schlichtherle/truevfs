/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.driver.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.NoSuchFileException;
import java.util.Date;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import net.java.truecommons.shed.BitField;
import net.java.truevfs.kernel.spec.FsAccessOption;
import static net.java.truevfs.kernel.spec.FsAccessOptions.NONE;
import net.java.truevfs.kernel.spec.FsEntry;
import net.java.truevfs.kernel.spec.FsEntryName;
import net.java.truevfs.kernel.spec.FsReadOnlyFileSystemException;
import static net.java.truevfs.kernel.spec.cio.Entry.Access.READ;
import static net.java.truevfs.kernel.spec.cio.Entry.Access.WRITE;
import static net.java.truevfs.kernel.spec.cio.Entry.Size.DATA;
import static net.java.truevfs.kernel.spec.cio.Entry.Type.FILE;
import net.java.truevfs.kernel.spec.cio.*;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpUriRequest;

/**
 * An HTTP(S) entry.
 *
 * @author Christian Schlichtherle
 */
@Immutable
public class HttpEntry extends FsEntry implements IoEntry<HttpEntry> {

    private final HttpController controller;
    private final String name;
    protected final URI uri;

    HttpEntry(  final HttpController controller,
                final FsEntryName name) {
        assert null != controller;
        this.controller = controller;
        this.name = name.toString();
        this.uri = controller.resolve(name).toUri();
    }

    final IoBufferPool getPool() {
        return controller.getPool();
    }

    private HttpResponse executeHead() throws IOException {
        return controller.executeHead(this);
    }

    private HttpResponse executeGet() throws IOException {
        return controller.executeGet(this);
    }

    protected HttpUriRequest newHead() {
        return new HttpHead(uri);
    }

    protected HttpUriRequest newGet() {
        return new HttpGet(uri);
    }

    private @CheckForNull String getHeaderField(String name) throws IOException {
        final Header header = executeHead().getLastHeader(name);
        return null == header ? null : header.getValue();
    }

    protected InputStream newInputStream() throws IOException {
        final HttpResponse response = executeGet();
        final HttpEntity entity = response.getEntity();
        if (null == entity)
            throw new NoSuchFileException(name, null, response.getStatusLine().toString());
        return entity.getContent();
    }

    protected OutputStream newOutputStream() throws IOException {
        throw new FsReadOnlyFileSystemException();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public BitField<Type> getTypes() {
        try {
            executeHead();
            return FILE_TYPE;
        } catch (IOException ex) {
            return NO_TYPES;
        }
    }

    @Override
    public boolean isType(final Type type) {
        return type == FILE && getTypes().is(FILE);
    }

    @Override
    public long getSize(final Size type) {
        if (DATA != type)
            return UNKNOWN;
        try {
            final String field = getHeaderField("content-length");
            if (null != field)
                return Long.parseLong(field);
        } catch (IOException ex) {
        }
        return UNKNOWN;
    }

    @Override
    @SuppressWarnings("deprecation")
    public long getTime(Access type) {
        if (WRITE != type)
            return UNKNOWN;
        try {
            final String field = getHeaderField("last-modified");
            if (null != field)
                return Date.parse(field);
        } catch (IllegalArgumentException | IOException ex) {
        }
        return UNKNOWN;
    }

    @Override
    public Boolean isPermitted(final Access type, final Entity entity) {
        if (READ != type)
            return null;
        try {
            executeHead();
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    @Override
    public @Nullable Set<String> getMembers() {
        return null;
    }

    @Override
    public final InputSocket<HttpEntry> input() {
        return input(NONE);
    }

    protected InputSocket<HttpEntry> input(BitField<FsAccessOption> options) {
        return new HttpInputSocket(options, this);
    }

    @Override
    public final OutputSocket<HttpEntry> output() {
        return output(NONE, null);
    }

    protected OutputSocket<HttpEntry> output(
            BitField<FsAccessOption> options,
            @CheckForNull Entry template) {
        return new HttpOutputSocket(options, this, template);
    }
}
