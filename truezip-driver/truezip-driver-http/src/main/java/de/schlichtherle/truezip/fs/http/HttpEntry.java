/*
 * Copyright (C) 2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.http;

import de.schlichtherle.truezip.entry.Entry;
import static de.schlichtherle.truezip.entry.Entry.Access.*;
import static de.schlichtherle.truezip.entry.Entry.Size.*;
import static de.schlichtherle.truezip.entry.Entry.Type.*;
import de.schlichtherle.truezip.fs.FsEntry;
import de.schlichtherle.truezip.fs.FsEntryName;
import de.schlichtherle.truezip.fs.FsInputOption;
import static de.schlichtherle.truezip.fs.FsInputOptions.*;
import de.schlichtherle.truezip.fs.FsOutputOption;
import static de.schlichtherle.truezip.fs.FsOutputOptions.*;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.socket.IOEntry;
import de.schlichtherle.truezip.socket.IOPool;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.Set;
import net.jcip.annotations.Immutable;

/**
 * An HTTP(S) entry.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@DefaultAnnotation(NonNull.class)
@edu.umd.cs.findbugs.annotations.SuppressWarnings("JCIP_FIELD_ISNT_FINAL_IN_IMMUTABLE_CLASS")
final class HttpEntry extends FsEntry implements IOEntry<HttpEntry> {

    private HttpController controller;
    private final String name;
    private final URL url;
    private volatile @CheckForNull URLConnection connection;

    HttpEntry(  final HttpController controller,
                final FsEntryName name) {
        assert null != controller;
        this.controller = controller;
        this.name = name.toString();
        try {
            this.url = controller.resolve(name).toUri().toURL();
        } catch (MalformedURLException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    IOPool<?> getPool() {
        return controller.getPool();
    }

    /** Returns the decorated URL. */
    URL getUrl() {
        return url;
    }

    URLConnection getConnection() throws IOException {
        final URLConnection connection = this.connection;
        return null != connection ? connection : (this.connection = url.openConnection());
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<Type> getTypes() {
        try {
            getConnection();
            return FILE_TYPE_SET;
        } catch (IOException failure) {
            return Collections.EMPTY_SET;
        }
    }

    @Override
    public boolean isType(final Type type) {
        if (FILE != type)
            return false;
        try {
            getConnection();
            return true;
        } catch (IOException failure) {
            return false;
        }
    }

    @Override
    public long getSize(final Size type) {
        try {
            if (DATA == type)
                return getConnection().getContentLength();
        } catch (IOException ex) {
        }
        return UNKNOWN;
    }

    @Override
    public long getTime(Access type) {
        try {
            if (WRITE == type)
                return getConnection().getLastModified();
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
        return getInputSocket(NO_INPUT_OPTIONS);
    }

    InputSocket<HttpEntry> getInputSocket(BitField<FsInputOption> options) {
        return new HttpInputSocket(this, options);
    }

    @Override
    public OutputSocket<HttpEntry> getOutputSocket() {
        return getOutputSocket(NO_OUTPUT_OPTIONS, null);
    }

    OutputSocket<HttpEntry> getOutputSocket(
            BitField<FsOutputOption> options,
            @CheckForNull Entry template) {
        return new HttpOutputSocket(this, options, template);
    }
}
