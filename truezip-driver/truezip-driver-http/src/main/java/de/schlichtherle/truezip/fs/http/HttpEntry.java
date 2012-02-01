/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.http;

import de.schlichtherle.truezip.entry.Entry;
import static de.schlichtherle.truezip.entry.Entry.Access.WRITE;
import static de.schlichtherle.truezip.entry.Entry.Size.DATA;
import static de.schlichtherle.truezip.entry.Entry.Type.FILE;
import de.schlichtherle.truezip.fs.FsEntry;
import de.schlichtherle.truezip.fs.FsEntryName;
import de.schlichtherle.truezip.fs.FsInputOption;
import static de.schlichtherle.truezip.fs.FsInputOptions.NO_INPUT_OPTIONS;
import de.schlichtherle.truezip.fs.FsOutputOption;
import static de.schlichtherle.truezip.fs.FsOutputOptions.NO_OUTPUT_OPTIONS;
import de.schlichtherle.truezip.socket.IOEntry;
import de.schlichtherle.truezip.socket.IOPool;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Collections;
import java.util.Date;
import java.util.Set;
import net.jcip.annotations.Immutable;
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
 * @version $Id$
 */
@Immutable
public class HttpEntry extends FsEntry implements IOEntry<HttpEntry> {

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

    final IOPool<?> getPool() {
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

    protected InputStream getInputStream() throws IOException {
        final HttpResponse response = executeGet();
        final HttpEntity entity = response.getEntity();
        if (null == entity)
            throw new FileNotFoundException(name + " (" + response.getStatusLine() + ")");
        return entity.getContent();
    }

    protected OutputStream getOutputStream() throws IOException {
        throw new ReadOnlyFileSystemTypeException();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<Type> getTypes() {
        try {
            if (null != executeHead())
                return FILE_TYPE_SET;
        } catch (IOException ex) {
        }
        return Collections.EMPTY_SET;
    }

    @Override
    public boolean isType(final Type type) {
        if (FILE != type)
            return false;
        try {
            if (null != executeHead())
                return true;
        } catch (IOException ex) {
        }
        return false;
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
        } catch (IllegalArgumentException ex) {
        } catch (IOException ex) {
        }
        return UNKNOWN;
    }

    @Override
    public @Nullable Set<String> getMembers() {
        return null;
    }

    @Override
    public InputSocket<HttpEntry> getInputSocket() {
        return newInputSocket(NO_INPUT_OPTIONS);
    }

    protected InputSocket<HttpEntry> newInputSocket(BitField<FsInputOption> options) {
        return new HttpInputSocket(this, options);
    }

    @Override
    public OutputSocket<HttpEntry> getOutputSocket() {
        return newOutputSocket(NO_OUTPUT_OPTIONS, null);
    }

    protected OutputSocket<HttpEntry> newOutputSocket(
            BitField<FsOutputOption> options,
            @CheckForNull Entry template) {
        return new HttpOutputSocket(this, options, template);
    }
}
