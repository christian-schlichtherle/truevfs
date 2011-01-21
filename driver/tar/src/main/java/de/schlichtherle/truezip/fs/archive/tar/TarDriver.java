/*
 * Copyright (C) 2006-2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.fs.archive.tar;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import de.schlichtherle.truezip.fs.FsConcurrentModel;
import de.schlichtherle.truezip.fs.archive.CharsetArchiveDriver;
import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.entry.Entry.Type;
import de.schlichtherle.truezip.fs.archive.MultiplexedArchiveOutputShop;
import de.schlichtherle.truezip.socket.IOPool;
import de.schlichtherle.truezip.socket.OutputShop;
import de.schlichtherle.truezip.socket.InputShop;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.socket.OutputSocket;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.CharConversionException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import net.jcip.annotations.Immutable;

import static de.schlichtherle.truezip.entry.Entry.Access.WRITE;
import static de.schlichtherle.truezip.entry.Entry.Size.DATA;

/**
 * An archive driver which builds TAR files.
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@DefaultAnnotation(NonNull.class)
public class TarDriver extends CharsetArchiveDriver<TarArchiveEntry> {

    /**
     * The default character set for entry names and comments, which is
     * {@code "US-ASCII"}.
     */
    public static final Charset TAR_CHARSET = Charset.forName("US-ASCII");

    private final IOPool<?> pool;

    public TarDriver(final IOPool<?> pool) {
        super(TAR_CHARSET);
        if (null == pool)
            throw new NullPointerException();
        this.pool = pool;
    }

    @Override
    public final IOPool<?> getPool() {
        return pool;
    }

    @Override
    public TarArchiveEntry newEntry(
            String name,
            final Type type,
            final Entry template)
    throws CharConversionException {
        assertEncodable(name);
        name = toZipOrTarEntryName(name, type);
        final TarArchiveEntry entry;
        if (null != template) {
            if (template instanceof TarArchiveEntry) {
                entry = newEntry(name, (TarArchiveEntry) template);
                entry.setName(name);
            } else {
                entry = newEntry(name);
                entry.setModTime(template.getTime(WRITE));
                entry.setSize(template.getSize(DATA));
            }
        } else {
            entry = newEntry(name);
        }
        return entry;
    }

    public TarArchiveEntry newEntry(String name) {
        return new TarArchiveEntry(name);
    }

    public TarArchiveEntry newEntry(String name, TarArchiveEntry template) {
        return new TarArchiveEntry(name, template);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link TarDriver} acquires a read only
     * file from the given socket and forwards the call to
     * {@link #newTarInputShop}.
     */
    @Override
    public TarInputShop newInputShop(FsConcurrentModel model, InputSocket<?> input)
    throws IOException {
        final InputStream in = input.newInputStream();
        try {
            return newTarInputShop(model, in);
        } finally {
            in.close();
        }
    }

    protected TarInputShop newTarInputShop(FsConcurrentModel model, InputStream in)
    throws IOException {
        return new TarInputShop(this, in);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation forwards the call to {@link #newTarOutputShop}
     * and wraps the result in a new {@link MultiplexedArchiveOutputShop}.
     */
    @Override
    public OutputShop<TarArchiveEntry> newOutputShop(
            FsConcurrentModel model,
            OutputSocket<?> output,
            InputShop<TarArchiveEntry> source)
    throws IOException {
        final OutputStream out = output.newOutputStream();
        try {
            return new MultiplexedArchiveOutputShop<TarArchiveEntry>(
                    newTarOutputShop(model, out, (TarInputShop) source),
                    getPool());
        } catch (IOException ex) {
            out.close();
            throw ex;
        }
    }

    protected TarOutputShop newTarOutputShop(
            FsConcurrentModel model, OutputStream out, TarInputShop source)
    throws IOException {
        return new TarOutputShop(this, out);
    }
}
