/*
 * Copyright (C) 2006-2010 Schlichtherle IT Services
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

package de.schlichtherle.truezip.io.archive.driver.tar;

import de.schlichtherle.truezip.io.archive.model.ArchiveModel;
import de.schlichtherle.truezip.io.archive.driver.AbstractArchiveDriver;
import de.schlichtherle.truezip.io.entry.Entry;
import de.schlichtherle.truezip.io.entry.Entry.Type;
import de.schlichtherle.truezip.io.archive.output.MultiplexedArchiveOutputShop;
import de.schlichtherle.truezip.io.socket.OutputShop;
import de.schlichtherle.truezip.io.socket.InputShop;
import de.schlichtherle.truezip.io.socket.InputSocket;
import de.schlichtherle.truezip.io.socket.OutputSocket;
import java.io.CharConversionException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static de.schlichtherle.truezip.io.entry.Entry.Access.WRITE;
import static de.schlichtherle.truezip.io.entry.Entry.Size.DATA;

/**
 * An archive driver which builds TAR files.
 * <p>
 * Instances of this class are immutable.
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class TarDriver
extends AbstractArchiveDriver<TarEntry> {

    private static final long serialVersionUID = 6622746562629104174L;

    /** Prefix for temporary files created by this driver. */
    static final String TEMP_FILE_PREFIX = "tzp-tar";

    /**
     * The character set to use for entry names, which is {@value}.
     * TAR files should actually be able to use the system's native character
     * set charset. However, the low level TAR code as of Ant 1.6.5 doesn't
     * support that, hence this constraint.
     */
    public static final String TAR_CHARSET = "US-ASCII";

    /**
     * Equivalent to {@link #TarDriver(String)
     * this(TAR_CHARSET)}.
     */
    public TarDriver() {
        this(TAR_CHARSET);
    }

    /**
     * Constructs a new TAR driver.
     *
     * @param charset The name of a character set to use for all entry names
     *        when reading or writing TAR files.
     *        <b>Warning:</b> Due to limitations in Apache's Ant code, using
     *        anything else than {@value #TAR_CHARSET} is currently not
     *        supported!
     */
    public TarDriver(String charset) {
        super(charset);
    }

    @Override
    public TarEntry newEntry(
            String name,
            final Type type,
            final Entry template)
    throws CharConversionException {
        name = toZipOrTarEntryName(name, type);
        final TarEntry entry;
        if (null != template) {
            if (template instanceof TarEntry) {
                entry = newEntry(name, (TarEntry) template);
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

    public TarEntry newEntry(String name) {
        return new TarEntry(name);
    }

    public TarEntry newEntry(String name, TarEntry template) {
        return new TarEntry(name, template);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link TarDriver} acquires a read only
     * file from the given socket and forwards the call to
     * {@link #newTarInputShop}.
     */
    @Override
    public TarInputShop newInputShop(ArchiveModel model, InputSocket<?> input)
    throws IOException {
        final InputStream in = input.newInputStream();
        try {
            return newTarInputShop(model, in);
        } finally {
            in.close();
        }
    }

    protected TarInputShop newTarInputShop(ArchiveModel model, InputStream in)
    throws IOException {
        return new TarInputShop(in);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation forwards the call to {@link #newTarOutputShop}
     * and wraps the result in a new {@link MultiplexedArchiveOutputShop}.
     */
    @Override
    public OutputShop<TarEntry> newOutputShop(
            ArchiveModel model,
            OutputSocket<?> output,
            InputShop<TarEntry> source)
    throws IOException {
        final OutputStream out = output.newOutputStream();
        try {
            return new MultiplexedArchiveOutputShop<TarEntry>(
                    newTarOutputShop(model, out, (TarInputShop) source));
        } catch (IOException ex) {
            out.close();
            throw ex;
        }
    }

    protected TarOutputShop newTarOutputShop(
            ArchiveModel model, OutputStream out, TarInputShop source)
    throws IOException {
        return new TarOutputShop(out);
    }
}
