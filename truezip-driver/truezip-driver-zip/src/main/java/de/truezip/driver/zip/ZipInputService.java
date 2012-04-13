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
import de.truezip.kernel.io.Source;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.NoSuchFileException;
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
    public ZipInputService(
            final FsModel model,
            final Source source,
            final ZipDriver driver)
    throws IOException {
        super(source, driver);
        this.driver = driver;
        if (null == (this.model = model)) {
            final NullPointerException ex = new NullPointerException();
            try {
                super.close();
            } catch (final IOException ex2) {
                ex.addSuppressed(ex2);
            }
            throw ex;
        }
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
    public InputSocket<ZipDriverEntry> input(final String name) {
        if (null == name)
            throw new NullPointerException();

        final class Input extends InputSocket<ZipDriverEntry> {
            @Override
            public ZipDriverEntry localTarget() throws IOException {
                final ZipDriverEntry entry = entry(name);
                if (null == entry)
                    throw new NoSuchFileException(name, null, "Entry not found!");
                if (entry.isDirectory())
                    throw new NoSuchFileException(name, null, "Cannot read directory entries!");
                return entry;
            }

            @Override
            public InputStream stream() throws IOException {
                final ZipDriverEntry local = localTarget();
                final Entry peer = peerTarget();
                final ZipDriverEntry zpeer = peer instanceof ZipDriverEntry
                        ? (ZipDriverEntry) peer
                        : null;
                final ZipDriver driver = ZipInputService.this.driver;
                return getInputStream(
                        local.getName(),
                        driver.check(local, ZipInputService.this),
                        null == zpeer
                        || 0 == zpeer.getSize()
                        || !driver.rdc(ZipInputService.this, local, zpeer));
            }
        } // Input

        return new Input();
    }
}
