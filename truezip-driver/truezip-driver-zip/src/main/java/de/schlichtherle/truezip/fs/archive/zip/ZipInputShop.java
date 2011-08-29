/*
 * Copyright (C) 2009-2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.fs.archive.zip;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.fs.FsModel;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.socket.InputShop;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.zip.RawZipFile;
import de.schlichtherle.truezip.zip.ZipCryptoParameters;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import net.jcip.annotations.NotThreadSafe;

/**
 * An input shop for reading ZIP files.
 *
 * @see     ZipOutputShop
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
@DefaultAnnotation(NonNull.class)
public class ZipInputShop
extends RawZipFile<ZipArchiveEntry>
implements InputShop<ZipArchiveEntry> {

    private final ZipDriver driver;
    private final FsModel model;
    private boolean appendee;
    private ZipCryptoParameters param;

    public ZipInputShop(
            final ZipDriver driver,
            final FsModel model,
            final ReadOnlyFile rof)
    throws IOException {
        super(rof, driver);
        this.driver = driver;
        this.model = model;
    }

    /**
     * Returns the file system model provided to the constructor.
     * 
     * @return The file system model provided to the constructor.
     * @since  TrueZIP 7.3
     */
    public FsModel getModel() {
        return model;
    }

    @Override
    protected ZipCryptoParameters getCryptoParameters() {
        ZipCryptoParameters param = this.param;
        if (null == param)
            this.param = param = driver.zipCryptoParameters(this);
        return param;
    }

    /**
     * Returns {@code true} if and only if the target archive file gets entries
     * appended to it.
     * Note that the implementation in the class {@link ZipInputShop} does not
     * use this property.
     * 
     * @return {@code true} if and only if the target archive file gets entries
     *         appended to it.
     */
    protected boolean isAppendee() {
        return appendee;
    }

    /**
     * Indicates whether or not the target archive file gets entries appended
     * to it.
     * 
     * @param appendee {@code true} if and only if the target archive file gets
     *        entries appended to it.
     */
    final void setAppendee(boolean appendee) {
        this.appendee = appendee;
    }

    @Override
    public int getSize() {
        return super.size();
    }

    @Override
    public InputSocket<ZipArchiveEntry> getInputSocket(final String name) {
        if (null == name)
            throw new NullPointerException();

        class Input extends InputSocket<ZipArchiveEntry> {
            @Override
            public ZipArchiveEntry getLocalTarget() throws IOException {
                final ZipArchiveEntry entry = getEntry(name);
                if (null == entry)
                    throw new FileNotFoundException(name + " (entry not found)");
                return entry;
            }

            @Override
            public ReadOnlyFile newReadOnlyFile() throws IOException {
                throw new UnsupportedOperationException(); // TODO: Support this feature for STORED entries.
            }

            @Override
            public InputStream newInputStream() throws IOException {
                final ZipArchiveEntry lt = getLocalTarget();
                final Entry pt = getPeerTarget();
                final ZipArchiveEntry zpt = pt instanceof ZipArchiveEntry
                        ? (ZipArchiveEntry) pt
                        : null;
                final ZipDriver driver = ZipInputShop.this.driver;
                return getInputStream(
                        lt.getName(),
                        driver.check(ZipInputShop.this, lt),
                        null == zpt || driver.process(ZipInputShop.this, lt, zpt));
            }
        } // Input

        return new Input();
    }
}
