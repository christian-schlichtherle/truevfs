/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.commons.zipdriver;

import global.namespace.truevfs.commons.cio.Entry;
import global.namespace.truevfs.commons.cio.InputContainer;
import global.namespace.truevfs.commons.cio.InputSocket;
import global.namespace.truevfs.commons.cio.OutputSocket;
import global.namespace.truevfs.commons.zip.AbstractZipFile;
import global.namespace.truevfs.commons.zip.ZipCryptoParameters;
import global.namespace.truevfs.kernel.api.FsInputSocketSource;
import global.namespace.truevfs.kernel.api.FsModel;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.NoSuchFileException;
import java.util.*;

/**
 * An input service for reading ZIP files.
 *
 * @param <E> the type of the ZIP driver entries.
 * @author Christian Schlichtherle
 * @see ZipOutputContainer
 */
public final class ZipInputContainer<E extends AbstractZipDriverEntry>
        extends AbstractZipFile<E> implements InputContainer<E> {

    private final AbstractZipDriver<E> driver;
    private final FsModel model;
    private ZipCryptoParameters param;

    public ZipInputContainer(
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
    public Collection<E> entries() {
        return new AbstractCollection<E>() {

            @Override
            public Iterator<E> iterator() {
                return ZipInputContainer.this.iterator();
            }

            @Override
            public int size() {
                return ZipInputContainer.this.size();
            }
        };
    }

    @Override
    public InputSocket<E> input(final String name) {
        Objects.requireNonNull(name);
        final class Input implements InputSocket<E> {

            @Override
            public E getTarget() throws IOException {
                final Optional<E> entry = entry(name);
                if (!entry.isPresent()) {
                    throw new NoSuchFileException(name, null, "Entry not found!");
                }
                if (entry.get().isDirectory()) {
                    throw new NoSuchFileException(name, null, "Cannot read directory entries!");
                }
                return entry.get();
            }

            @Override
            public InputStream stream(Optional<? extends OutputSocket<? extends Entry>> output) throws IOException {
                final E local = getTarget();
                final Entry peer = output.isPresent() ? output.get().getTarget() : null;
                final AbstractZipDriverEntry zpeer = peer instanceof AbstractZipDriverEntry
                        ? (AbstractZipDriverEntry) peer
                        : null;
                final AbstractZipDriver<E> driver = ZipInputContainer.this.driver;
                return getInputStream(
                        local.getName(),
                        driver.check(local, ZipInputContainer.this),
                        null == zpeer
                                || 0 == zpeer.getSize()
                                || !driver.rdc(ZipInputContainer.this, local, zpeer));
            }
        }
        return new Input();
    }
}
