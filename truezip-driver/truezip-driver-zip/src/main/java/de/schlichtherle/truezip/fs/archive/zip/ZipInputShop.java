/*
 * Copyright (C) 2004-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.archive.zip;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.fs.FsModel;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.socket.InputShop;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.zip.RawZipFile;
import de.schlichtherle.truezip.zip.ZipCryptoParameters;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * An input shop for reading ZIP files.
 *
 * @see    ZipOutputShop
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public class ZipInputShop
extends RawZipFile<ZipDriverEntry>
implements InputShop<ZipDriverEntry> {

    private final ZipDriver driver;
    private final FsModel model;
    private boolean appendee;
    private ZipCryptoParameters param;

    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    public ZipInputShop(
            final ZipDriver driver,
            final FsModel model,
            final @WillCloseWhenClosed ReadOnlyFile rof)
    throws IOException {
        super(rof, driver);
        if (null == model)
            throw new NullPointerException();
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
    public InputSocket<ZipDriverEntry> getInputSocket(final String name) {
        if (null == name)
            throw new NullPointerException();

        class Input extends InputSocket<ZipDriverEntry> {
            @Override
            public ZipDriverEntry getLocalTarget() throws IOException {
                final ZipDriverEntry entry = getEntry(name);
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
                final ZipDriverEntry lt = getLocalTarget();
                final Entry pt = getPeerTarget();
                final ZipDriverEntry zpt = pt instanceof ZipDriverEntry
                        ? (ZipDriverEntry) pt
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
