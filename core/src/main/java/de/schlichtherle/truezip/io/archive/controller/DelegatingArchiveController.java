/*
 * Copyright (C) 2010 Schlichtherle IT Services
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
package de.schlichtherle.truezip.io.archive.controller;

import de.schlichtherle.truezip.io.socket.entry.FilterCommonEntry;
import java.util.Set;
import java.net.URI;
import de.schlichtherle.truezip.io.archive.controller.ArchiveController.SyncOption;
import de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystem;
import de.schlichtherle.truezip.io.socket.entry.CommonEntry;
import de.schlichtherle.truezip.io.socket.entry.CommonEntry.Type;
import de.schlichtherle.truezip.io.socket.entry.CommonEntry.Access;
import de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystemEntry;
import de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystem.Entry;
import de.schlichtherle.truezip.io.socket.output.CommonOutputSocket;
import de.schlichtherle.truezip.io.socket.input.CommonInputSocket;
import de.schlichtherle.truezip.io.archive.driver.ArchiveEntry;
import de.schlichtherle.truezip.util.BitField;
import java.io.FileNotFoundException;
import java.io.IOException;
import javax.swing.Icon;

import static de.schlichtherle.truezip.io.socket.entry.CommonEntry.Type.DIRECTORY;
import static de.schlichtherle.truezip.io.socket.entry.CommonEntry.Type.FILE;
import static de.schlichtherle.truezip.io.socket.entry.CommonEntry.Type.SPECIAL;
import static de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystems.isRoot;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
final class DelegatingArchiveController<AE extends ArchiveEntry>
extends ArchiveController<AE> {

    private final ArchiveController<AE> target;

    /** The archive controller of the enclosing archive file, if any. */
    private final ArchiveController<?> enclController;

    DelegatingArchiveController(
            ArchiveModel<AE> model,
            ArchiveController<AE> target) {
        super(model);
        this.target = target;
        final ArchiveModel<?> enclModel = model.getEnclModel();
        enclController = null == enclModel
                ? null
                : ArchiveControllers.getController(enclModel.getMountPoint());
        assert (null == enclModel) == (null == enclController);
    }

    final ArchiveController<?> getEnclController() {
        return enclController;
    }

    @Override
    public CommonInputSocket<? extends CommonEntry> getInputSocket(String path)
    throws IOException {
        try {
            return target.getInputSocket(path);
        } catch (ArchiveEntryFalsePositiveException ex) {
            return getEnclController().getInputSocket(getEnclPath(path));
        }
    }

    @Override
    public CommonOutputSocket<? extends CommonEntry> getOutputSocket(
            final String path,
            final BitField<IOOption> options)
    throws IOException {
        try {
            return target.getOutputSocket(path, options);
        } catch (ArchiveEntryFalsePositiveException ex) {
            return getEnclController().getOutputSocket(getEnclPath(path), options);
        }
    }

    @Override
    public Icon getOpenIcon()
    throws FalsePositiveException {
        try {
            return target.getOpenIcon();
        } catch (ArchiveEntryFalsePositiveException ex) {
            return getEnclController().getOpenIcon();
        }
    }

    @Override
    public Icon getClosedIcon()
    throws FalsePositiveException {
        try {
            return target.getClosedIcon();
        } catch (ArchiveEntryFalsePositiveException ex) {
            return getEnclController().getClosedIcon();
        }
    }

    @Override
    public final boolean isReadOnly()
    throws FalsePositiveException {
        try {
            return target.isReadOnly();
        } catch (ArchiveEntryFalsePositiveException ex) {
            return getEnclController().isReadOnly();
        }
    }

    @Override
    public final Entry<?> getEntry(final String path)
    throws FalsePositiveException {
        try {
            return target.getEntry(path);
        } catch (FileArchiveEntryFalsePositiveException ex) {
            /** @see ArchiveDriver#newInputShop! */
            if (isRoot(path) && ex.getCause() instanceof FileNotFoundException)
                return new SpecialFileEntry<ArchiveEntry>(getEnclController()
                        .getEntry(getEnclPath(path))
                        .getTarget()); // the exception asserts that the entry exists as a file!
        } catch (ArchiveEntryFalsePositiveException ex) {
        }
        return getEnclController().getEntry(getEnclPath(path));
    }

    private static final class SpecialFileEntry<AE extends ArchiveEntry>
    extends FilterCommonEntry<AE>
    implements Entry<AE> {
        SpecialFileEntry(AE target) {
            super(target);
        }

        @Override
        public Type getType() {
            assert FILE == super.getType();
            return SPECIAL;
        }

        @Override
        public Set<String> list() {
            return null;
        }

        @Override
        public AE getTarget() {
            return target;
        }
    }

    @Override
    public final boolean isReadable(final String path)
    throws FalsePositiveException {
        try {
            return target.isReadable(path);
        } catch (ArchiveEntryFalsePositiveException ex) {
            return getEnclController().isReadable(getEnclPath(path));
        }
    }

    @Override
    public final boolean isWritable(final String path)
    throws FalsePositiveException {
        try {
            return target.isWritable(path);
        } catch (ArchiveEntryFalsePositiveException ex) {
            return getEnclController().isWritable(getEnclPath(path));
        }
    }

    @Override
    public final void setReadOnly(final String path)
    throws IOException {
        try {
            target.setReadOnly(path);
        } catch (ArchiveEntryFalsePositiveException ex) {
            getEnclController().setReadOnly(getEnclPath(path));
        }
    }

    @Override
    public final void setTime(
            final String path,
            final BitField<Access> types,
            final long value)
    throws IOException {
        try {
            target.setTime(path, types, value);
        } catch (ArchiveEntryFalsePositiveException ex) {
            getEnclController().setTime(getEnclPath(path), types, value);
        }
    }

    @Override
    public final void mknod(
            final String path,
            final Type type,
            final CommonEntry template,
            final BitField<IOOption> options)
    throws IOException {
        try {
            target.mknod(path, type, template, options);
        } catch (ArchiveEntryFalsePositiveException ex) {
            getEnclController().mknod(getEnclPath(path), type, template, options);
        }
    }

    @Override
    @SuppressWarnings("ThrowableInitCause")
    public final void unlink(
            final String path,
            final BitField<IOOption> options)
    throws IOException {
        try {
            target.unlink(path, options);
            return;
        } catch (FileArchiveEntryFalsePositiveException ex) {
            /** @see ArchiveDriver#newInputShop! */
            // FIXME: What if we remove this special case? We could probably delete a RAES encrypted ZIP file with an unknown password. Would we want this?
            if (isRoot(path)) {
                final ArchiveFileSystemEntry entry = getEnclController().getEntry(getEnclPath(path));
                if (null == entry || entry.getType() != DIRECTORY
                    && ex.getCause() instanceof FileNotFoundException) {
                    throw (IOException) new IOException(ex.toString()).initCause(ex); // mask!
                }
            }
        } catch (ArchiveEntryFalsePositiveException ex) {
        }
        getEnclController().unlink(getEnclPath(path), options);
    }

    @Override
    ArchiveFileSystem<AE> autoMount(boolean autoCreate, boolean createParents)
    throws IOException {
        return target.autoMount(autoCreate, createParents);
    }

    @Override
    boolean hasNewData(String path) {
        return target.hasNewData(path);
    }

    @Override
    public void sync(ArchiveSyncExceptionBuilder builder, BitField<SyncOption> options)
    throws ArchiveSyncException {
        target.sync(builder, options);
    }
}
