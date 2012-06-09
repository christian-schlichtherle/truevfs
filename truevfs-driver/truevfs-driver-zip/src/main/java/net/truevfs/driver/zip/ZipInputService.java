/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.zip;

import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.NoSuchFileException;
import java.util.Objects;
import javax.annotation.concurrent.NotThreadSafe;
import net.truevfs.driver.zip.io.RawZipFile;
import net.truevfs.driver.zip.io.ZipCryptoParameters;
import net.truevfs.kernel.FsInputSocketSource;
import net.truevfs.kernel.FsModel;
import net.truevfs.kernel.cio.*;

/**
 * An input service for reading ZIP files.
 *
 * @see    ZipOutputService
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public final class ZipInputService
extends RawZipFile<ZipDriverEntry>
implements InputService<ZipDriverEntry> {

    private final ZipDriver driver;
    private final FsModel model;
    private ZipCryptoParameters param;

    @CreatesObligation
    public ZipInputService(
            final FsModel model,
            final FsInputSocketSource source,
            final ZipDriver driver)
    throws IOException {
        super(source, driver);
        this.driver = driver;
        if (null == (this.model = model)) {
            final NullPointerException ex = new NullPointerException();
            try {
                super.close();
            } catch (final Throwable ex2) {
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

    @Override
    public InputSocket<ZipDriverEntry> input(final String name) {
        Objects.requireNonNull(name);

        final class Input extends AbstractInputSocket<ZipDriverEntry> {
            @Override
            public ZipDriverEntry target() throws IOException {
                final ZipDriverEntry entry = entry(name);
                if (null == entry)
                    throw new NoSuchFileException(name, null, "Entry not found!");
                if (entry.isDirectory())
                    throw new NoSuchFileException(name, null, "Cannot read directory entries!");
                return entry;
            }

            @Override
            public InputStream stream(OutputSocket<? extends Entry> output)
            throws IOException {
                final ZipDriverEntry local = target();
                final Entry peer = target(output);
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
