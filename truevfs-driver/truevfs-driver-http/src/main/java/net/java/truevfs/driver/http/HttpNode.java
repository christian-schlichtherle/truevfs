/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.driver.http;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
import net.java.truecommons.cio.*;
import static net.java.truecommons.cio.Entry.Access.READ;
import static net.java.truecommons.cio.Entry.Access.WRITE;
import static net.java.truecommons.cio.Entry.Size.DATA;
import static net.java.truecommons.cio.Entry.Type.FILE;
import net.java.truecommons.shed.BitField;
import net.java.truevfs.kernel.spec.FsAbstractNode;
import net.java.truevfs.kernel.spec.FsAccessOption;
import static net.java.truevfs.kernel.spec.FsAccessOptions.NONE;
import net.java.truevfs.kernel.spec.FsNodeName;
import net.java.truevfs.kernel.spec.FsReadOnlyFileSystemException;
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
public class HttpNode extends FsAbstractNode implements IoEntry<HttpNode> {

    private final HttpController controller;
    private final String name;
    protected final URI uri;

    HttpNode(   final HttpController controller,
                final FsNodeName name) {
        assert null != controller;
        this.controller = controller;
        this.name = name.toString();
        this.uri = controller.resolve(name).getUri();
    }

    final IoBufferPool getPool() { return controller.getPool(); }

    private HttpResponse executeHead() throws IOException {
        return controller.executeHead(this);
    }

    private HttpResponse executeGet() throws IOException {
        return controller.executeGet(this);
    }

    protected HttpUriRequest newHead() { return new HttpHead(uri); }

    protected HttpUriRequest newGet() { return new HttpGet(uri); }

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
        throw new FsReadOnlyFileSystemException(controller.getMountPoint());
    }

    @Override
    public String getName() { return name; }

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
    @SuppressFBWarnings("NP_BOOLEAN_RETURN_NULL")
    public Boolean isPermitted(final Access type, final Entity entity) {
        if (READ != type) return null;
        try {
            executeHead();
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    @Override
    public @Nullable Set<String> getMembers() { return null; }

    @Override
    public final InputSocket<HttpNode> input() { return input(NONE); }

    protected InputSocket<HttpNode> input(BitField<FsAccessOption> options) {
        return new HttpInputSocket(options, this);
    }

    @Override
    public final OutputSocket<HttpNode> output() { return output(NONE, null); }

    protected OutputSocket<HttpNode> output(
            BitField<FsAccessOption> options,
            @CheckForNull Entry template) {
        return new HttpOutputSocket(options, this, template);
    }
}
