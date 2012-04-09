/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip;

import de.truezip.driver.zip.io.RawFile;
import de.truezip.driver.zip.io.ZipCryptoParameters;
import de.truezip.kernel.FsModel;
import de.truezip.kernel.cio.Entry;
import de.truezip.kernel.cio.InputService;
import de.truezip.kernel.cio.InputSocket;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * An input service for reading ZIP files.
 *
 * @see    ZipOutputService
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public final class ZipInputService
extends RawFile<ZipDriverEntry>
implements InputService<ZipDriverEntry> {

    private final ZipDriver driver;
    private final FsModel model;
    private boolean appendee;
    private ZipCryptoParameters param;

    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    public ZipInputService(
            final ZipDriver driver,
            final FsModel model,
            final @WillCloseWhenClosed SeekableByteChannel channel)
    throws IOException {
        super(channel, driver);
        if (null == model)
            throw new NullPointerException();
        this.driver = driver;
        this.model = model;
    }

    /**
     * Returns the file system model provided to the constructor.
     * 
     * @return The file system model provided to the constructor.
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
     * Note that the implementation in the class {@link ZipInputService} does not
     * use this property.
     * 
     * @return {@code true} if and only if the target archive file gets entries
     *         appended to it.
     */
    boolean isAppendee() {
        return appendee;
    }

    /**
     * Indicates whether or not the target archive file gets entries appended
     * to it.
     * 
     * @param appendee {@code true} if and only if the target archive file gets
     *        entries appended to it.
     */
    void setAppendee(boolean appendee) {
        this.appendee = appendee;
    }

    @Override
    public InputSocket<ZipDriverEntry> getInputSocket(final String name) {
        if (null == name)
            throw new NullPointerException();

        final class Input extends InputSocket<ZipDriverEntry> {
            @Override
            public ZipDriverEntry getLocalTarget() throws IOException {
                final ZipDriverEntry entry = getEntry(name);
                if (null == entry)
                    throw new FileNotFoundException(name + " (entry not found)");
                return entry;
            }

            @Override
            public InputStream newStream() throws IOException {
                final ZipDriverEntry lt = getLocalTarget();
                final Entry pt = getPeerTarget();
                final ZipDriverEntry zpt = pt instanceof ZipDriverEntry
                        ? (ZipDriverEntry) pt
                        : null;
                final ZipDriver driver = ZipInputService.this.driver;
                return getInputStream(
                        lt.getName(),
                        driver.check(ZipInputService.this, lt),
                        null == zpt || driver.process(ZipInputService.this, lt, zpt));
            }
        } // Input

        return new Input();
    }
}
