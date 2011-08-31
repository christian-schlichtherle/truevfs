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
package de.schlichtherle.truezip.fs.http;

import de.schlichtherle.truezip.entry.Entry;
import static de.schlichtherle.truezip.entry.Entry.Access.*;
import static de.schlichtherle.truezip.entry.Entry.Size.*;
import static de.schlichtherle.truezip.entry.Entry.Type.*;
import de.schlichtherle.truezip.fs.FsEntry;
import de.schlichtherle.truezip.fs.FsEntryName;
import de.schlichtherle.truezip.fs.FsMountPoint;
import de.schlichtherle.truezip.fs.FsOutputOption;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.socket.IOEntry;
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

    private static final BitField<FsOutputOption> NO_OUTPUT_OPTIONS
            = BitField.noneOf(FsOutputOption.class);

    private final String name;
    private final URL url;
    private HttpController controller;
    private volatile @CheckForNull URLConnection connection;

    HttpEntry(  final FsMountPoint mountPoint,
                final FsEntryName name,
                final HttpController controller) {
        this.name = name.toString();
        try {
            this.url = mountPoint.resolve(name).toUri().toURL();
        } catch (MalformedURLException ex) {
            throw new IllegalArgumentException(ex);
        }
        assert null != controller;
        this.controller = controller;
    }

    HttpController getController() {
        return controller;
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
        return new HttpInputSocket(this);
    }

    @Override
    public OutputSocket<HttpEntry> getOutputSocket() {
        return new HttpOutputSocket(NO_OUTPUT_OPTIONS, null, this);
    }

    public OutputSocket<HttpEntry> getOutputSocket(
            BitField<FsOutputOption> options,
            @CheckForNull Entry template) {
        return new HttpOutputSocket(options, template, this);
    }
}
