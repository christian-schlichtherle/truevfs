/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.archive.zip;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.fs.*;
import de.schlichtherle.truezip.key.KeyManager;
import de.schlichtherle.truezip.key.KeyProvider;
import de.schlichtherle.truezip.key.SafeKeyManager;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.ExceptionHandler;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import javax.swing.Icon;
import net.jcip.annotations.ThreadSafe;

/**
 * This file system controller decorates another file system controller in
 * order to manage the keys required for accessing encrypted ZIP files.
 * 
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@DefaultAnnotation(NonNull.class)
public abstract class KeyManagerController<D extends ZipDriver>
extends FsDecoratingController<FsModel, FsController<?>> {

    protected final D driver;
    private volatile KeyManager<?> manager;

    /**
     * Constructs a new key manager controller.
     *
     * @param controller the non-{@code null} file system controller to
     *        decorate.
     * @param driver the ZIP driver.
     */
    protected KeyManagerController(
            final FsController<?> controller,
            final D driver) {
        super(controller);
        if (null == driver)
            throw new NullPointerException();
        this.driver = driver;
    }

    protected abstract Class<?> getKeyType();

    protected abstract Class<? extends IOException> getKeyExceptionType();

    private KeyManager<?> getKeyManager() {
        KeyManager<?> manager = this.manager;
        if (null == manager)
            this.manager = manager = driver.getKeyManagerProvider().get(
                    getKeyType());
        return manager;
    }

    @Override
    public void unlink(final FsEntryName name, BitField<FsOutputOption> options)
    throws IOException {
        try {
            delegate.unlink(name, options);
        } catch (final FsFalsePositiveException ex) {
            // If the false positive exception is caused by a key exception,
            // then throw this cause instead in order to avoid delegating
            // this operation to the parent file system.
            // This prevents the application from inadvertently deleting an
            // encrypted ZIP file just because e.g. the user has cancelled key
            // prompting.
            final IOException cause = ex.getCause();
            if (null != cause
                    && getKeyExceptionType().isAssignableFrom(cause.getClass()))
                throw cause;
            throw ex;
        }
        if (name.isRoot())
            getKeyManager().removeKeyProvider(
                    driver.resourceUri(getModel(), name.toString()));
    }

    @Override
    public <X extends IOException> void
    sync(   final BitField<FsSyncOption> options,
            final ExceptionHandler<? super FsSyncException, X> handler)
    throws X {
        delegate.sync(options, handler);
        final KeyManager<?> manager = getKeyManager();
        final URI resource = driver.mountPointUri(getModel());
        final KeyProvider<?> provider;
        if (manager instanceof SafeKeyManager) {
            // Don't create a key provider if there wasn't one mapped already.
            provider = ((SafeKeyManager) manager).getMappedKeyProvider(resource);
        } else {
            // TODO: This might create a memory leak.
            // It's unlikely that a third party implements the KeyManager
            // interface and does NOT use extend the SafeKeyManager class,
            // though.
            provider = manager.getKeyProvider(resource);
        }
        if (null != provider)
            driver.getKeyProviderSyncStrategy().sync(provider);
    }

    @Override
    public Icon getOpenIcon() throws IOException {
        return delegate.getOpenIcon();
    }

    @Override
    public Icon getClosedIcon() throws IOException {
        return delegate.getClosedIcon();
    }

    @Override
    public boolean isReadOnly() throws IOException {
        return delegate.isReadOnly();
    }

    @Override
    public FsEntry getEntry(FsEntryName name)
    throws IOException {
        return delegate.getEntry(name);
    }

    @Override
    public boolean isReadable(FsEntryName name) throws IOException {
        return delegate.isReadable(name);
    }

    @Override
    public boolean isWritable(FsEntryName name) throws IOException {
        return delegate.isWritable(name);
    }

    @Override
    public boolean isExecutable(FsEntryName name) throws IOException {
        return delegate.isExecutable(name);
    }

    @Override
    public void setReadOnly(FsEntryName name) throws IOException {
        delegate.setReadOnly(name);
    }

    @Override
    public boolean setTime(
            FsEntryName name,
            Map<Entry.Access, Long> times,
            BitField<FsOutputOption> options)
    throws IOException {
        return delegate.setTime(name, times, options);
    }

    @Override
    public boolean setTime(
            FsEntryName name,
            BitField<Entry.Access> types,
            long value,
            BitField<FsOutputOption> options)
    throws IOException {
        return delegate.setTime(name, types, value, options);
    }

    @Override
    public InputSocket<?>
    getInputSocket( FsEntryName name,
                    BitField<FsInputOption> options) {
        return delegate.getInputSocket(name, options);
    }

    @Override
    public OutputSocket<?>
    getOutputSocket(    FsEntryName name,
                        BitField<FsOutputOption> options,
                        Entry template) {
        return delegate.getOutputSocket(name, options, template);
    }

    @Override
    public void
    mknod(  FsEntryName name,
            Entry.Type type,
            BitField<FsOutputOption> options,
            Entry template)
    throws IOException {
        delegate.mknod(name, type, options, template);
    }
}
