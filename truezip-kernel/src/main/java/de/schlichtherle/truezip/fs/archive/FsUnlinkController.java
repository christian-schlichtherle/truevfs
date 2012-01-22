/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.archive;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.entry.Entry.Access;
import de.schlichtherle.truezip.entry.Entry.Type;
import de.schlichtherle.truezip.fs.*;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.ExceptionHandler;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.util.Map;
import javax.swing.Icon;
import net.jcip.annotations.NotThreadSafe;

/**
 * A decorating file system controller which unlinks the target archive file
 * from the parent file system if and only if the virtual root directory has
 * been successfully unlinked from its federated file system before.
 * 
 * @since   TrueZIP 7.4.4
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
@DefaultAnnotation(NonNull.class)
final class FsUnlinkController
extends FsDecoratingController<FsLockModel, FsController<? extends FsLockModel>> {

    /**
     * Constructs a new file system unlink controller.
     *
     * @param controller the decorated file system controller.
     */
    FsUnlinkController(FsController<? extends FsLockModel> controller) {
        super(controller);
        assert null != super.getParent();
    }

    @Override
    public void unlink( final FsEntryName name,
                        final BitField<FsOutputOption> options)
    throws IOException {
        delegate.unlink(name, options);
        if (name.isRoot()) {
            // We have just removed the virtual root directory of a
            // federated file system, i.e. an archive file.
            // Now unlink the target archive file from the parent file system.
            getParent().unlink(
                    getMountPoint().getPath().resolve(name).getEntryName(),
                    options);
        }
    }

    @Override
    @Deprecated
    public Icon getClosedIcon() throws IOException {
        return delegate.getClosedIcon();
    }

    @Override
    public FsEntry getEntry(FsEntryName name) throws IOException {
        return delegate.getEntry(name);
    }

    @Override
    public InputSocket<?> getInputSocket(FsEntryName name, BitField<FsInputOption> options) {
        return delegate.getInputSocket(name, options);
    }

    @Override
    @Deprecated
    public Icon getOpenIcon() throws IOException {
        return delegate.getOpenIcon();
    }

    @Override
    public OutputSocket<?> getOutputSocket(FsEntryName name, BitField<FsOutputOption> options, Entry template) {
        return delegate.getOutputSocket(name, options, template);
    }

    @Override
    public boolean isExecutable(FsEntryName name) throws IOException {
        return delegate.isExecutable(name);
    }

    @Override
    public boolean isReadOnly() throws IOException {
        return delegate.isReadOnly();
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
    public void mknod(FsEntryName name, Type type, BitField<FsOutputOption> options, Entry template) throws IOException {
        delegate.mknod(name, type, options, template);
    }

    @Override
    public void setReadOnly(FsEntryName name) throws IOException {
        delegate.setReadOnly(name);
    }

    @Override
    public boolean setTime(FsEntryName name, Map<Access, Long> times, BitField<FsOutputOption> options) throws IOException {
        return delegate.setTime(name, times, options);
    }

    @Override
    public boolean setTime(FsEntryName name, BitField<Access> types, long value, BitField<FsOutputOption> options) throws IOException {
        return delegate.setTime(name, types, value, options);
    }

    @Override
    public <X extends IOException> void sync(BitField<FsSyncOption> options, ExceptionHandler<? super FsSyncException, X> handler) throws X {
        delegate.sync(options, handler);
    }
}
