/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.zipdriver;

import global.namespace.truevfs.comp.cio.*;
import global.namespace.truevfs.comp.zip.AbstractZipFile;
import global.namespace.truevfs.comp.zip.ZipCryptoParameters;
import global.namespace.truevfs.kernel.spec.FsInputSocketSource;
import global.namespace.truevfs.kernel.spec.FsModel;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.NoSuchFileException;
import java.util.Objects;
import java.util.Optional;

/**
 * An input service for reading ZIP files.
 *
 * @param  <E> the type of the ZIP driver entries.
 * @see    ZipOutputService
 * @author Christian Schlichtherle
 */
public final class ZipInputService<E extends AbstractZipDriverEntry>
extends AbstractZipFile<E> implements InputService<E> {

    private final AbstractZipDriver<E> driver;
    private final FsModel model;
    private ZipCryptoParameters param;

    public ZipInputService(
            final FsModel model,
            final FsInputSocketSource source,
            final AbstractZipDriver<E> driver)
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
    public InputSocket<E> input(final String name) {
        Objects.requireNonNull(name);
        final class Input extends AbstractInputSocket<E> {

            @Override
            public E target() throws IOException {
                final E entry = entry(name);
                if (null == entry)
                    throw new NoSuchFileException(name, null, "Entry not found!");
                if (entry.isDirectory())
                    throw new NoSuchFileException(name, null, "Cannot read directory entries!");
                return entry;
            }

            @Override
            public InputStream stream(Optional<? extends OutputSocket<? extends Entry>> output) throws IOException {
                final E local = target();
                final Entry peer = output.isPresent() ? output.get().target() : null;
                final AbstractZipDriverEntry zpeer = peer instanceof AbstractZipDriverEntry
                        ? (AbstractZipDriverEntry) peer
                        : null;
                final AbstractZipDriver<E> driver = ZipInputService.this.driver;
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
