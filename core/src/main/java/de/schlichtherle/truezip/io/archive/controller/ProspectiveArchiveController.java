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
import de.schlichtherle.truezip.io.socket.entry.CommonEntry;
import de.schlichtherle.truezip.io.socket.entry.CommonEntry.Type;
import de.schlichtherle.truezip.io.socket.entry.CommonEntry.Access;
import de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystemEntry;
import de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystem.Entry;
import de.schlichtherle.truezip.io.socket.output.CommonOutputSocket;
import de.schlichtherle.truezip.io.socket.input.CommonInputSocket;
import de.schlichtherle.truezip.io.archive.entry.ArchiveEntry;
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
final class ProspectiveArchiveController extends ArchiveController {

    private final ArchiveController controller;

    /** The archive controller of the enclosing archive file, if any. */
    private final ArchiveController enclController;

    ProspectiveArchiveController(ArchiveModel model, ArchiveController controller) {
        super(model);
        this.controller = controller;
        this.enclController = super.getEnclController();
    }

    @Override
    ArchiveController getEnclController() {
        return enclController;
    }

    @Override
    public Icon getOpenIcon()
    throws FalsePositiveEntryException {
        try {
            return controller.getOpenIcon();
        } catch (FalsePositiveEnclosedEntryException ex) {
            return getEnclController().getOpenIcon();
        }
    }

    @Override
    public Icon getClosedIcon()
    throws FalsePositiveEntryException {
        try {
            return controller.getClosedIcon();
        } catch (FalsePositiveEnclosedEntryException ex) {
            return getEnclController().getClosedIcon();
        }
    }

    @Override
    public boolean isReadOnly()
    throws FalsePositiveEntryException {
        try {
            return controller.isReadOnly();
        } catch (FalsePositiveEnclosedEntryException ex) {
            return getEnclController().isReadOnly();
        }
    }

    @Override
    public Entry<?> getEntry(final String path)
    throws FalsePositiveEntryException {
        try {
            return controller.getEntry(path);
        } catch (FalsePositiveEnclosedFileException ex) {
            /** @see ArchiveDriver#newInputShop! */
            if (isRoot(path) && ex.getCause() instanceof FileNotFoundException)
                return new SpecialFileEntry<ArchiveEntry>(getEnclController()
                        .getEntry(getEnclPath(path))
                        .getTarget()); // the exception asserts that the entry exists as a file!
            // Fall through!
        } catch (FalsePositiveEnclosedEntryException ex) {
        }
        return getEnclController().getEntry(getEnclPath(path));
    }

    private static final class SpecialFileEntry<AE extends ArchiveEntry>
    extends FilterCommonEntry<AE>
    implements Entry<AE> {
        SpecialFileEntry(AE entry) {
            super(entry);
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
            return entry;
        }
    }

    @Override
    public boolean isReadable(final String path)
    throws FalsePositiveEntryException {
        try {
            return controller.isReadable(path);
        } catch (FalsePositiveEnclosedEntryException ex) {
            return getEnclController().isReadable(getEnclPath(path));
        }
    }

    @Override
    public boolean isWritable(final String path)
    throws FalsePositiveEntryException {
        try {
            return controller.isWritable(path);
        } catch (FalsePositiveEnclosedEntryException ex) {
            return getEnclController().isWritable(getEnclPath(path));
        }
    }

    @Override
    public void setReadOnly(final String path)
    throws IOException {
        try {
            controller.setReadOnly(path);
        } catch (FalsePositiveEnclosedEntryException ex) {
            getEnclController().setReadOnly(getEnclPath(path));
        }
    }

    @Override
    public void setTime(
            final String path,
            final BitField<Access> types,
            final long value)
    throws IOException {
        try {
            controller.setTime(path, types, value);
        } catch (FalsePositiveEnclosedEntryException ex) {
            getEnclController().setTime(getEnclPath(path), types, value);
        }
    }

    @Override
    public CommonInputSocket<?> newInputSocket(String path)
    throws IOException {
        try {
            return controller.newInputSocket(path);
        } catch (FalsePositiveEnclosedEntryException ex) {
            return getEnclController().newInputSocket(getEnclPath(path));
        }
    }

    @Override
    public CommonOutputSocket<?> newOutputSocket(
            final String path,
            final BitField<OutputOption> options)
    throws IOException {
        try {
            return controller.newOutputSocket(path, options);
        } catch (FalsePositiveEnclosedEntryException ex) {
            return getEnclController().newOutputSocket(getEnclPath(path), options);
        }
    }

    @Override
    public void mknod(
            final String path,
            final Type type,
            final CommonEntry template,
            final BitField<OutputOption> options)
    throws IOException {
        try {
            controller.mknod(path, type, template, options);
        } catch (FalsePositiveEnclosedEntryException ex) {
            getEnclController().mknod(getEnclPath(path), type, template, options);
        }
    }

    @Override
    public void unlink(
            final String path,
            final BitField<OutputOption> options)
    throws IOException {
        try {
            controller.unlink(path, options);
            return;
        } catch (FalsePositiveEnclosedFileException ex) {
            /** @see ArchiveDriver#newInputShop! */
            // FIXME: Check if needed anymore!
            // What if we remove this special case? We could probably delete a RAES encrypted ZIP file with an unknown password. Would we want this?
            if (isRoot(path)) {
                final ArchiveFileSystemEntry entry = getEnclController().getEntry(getEnclPath(path));
                if (null == entry || DIRECTORY != entry.getType() // TODO: Redundant check?
                    && ex.getCause() instanceof FileNotFoundException) {
                    throw (IOException) new IOException(ex.toString()).initCause(ex); // mask!
                }
            }
            // Fall through!
        } catch (FalsePositiveEnclosedEntryException ex) {
        }
        getEnclController().unlink(getEnclPath(path), options);
    }

    @Override
    public void sync(ArchiveSyncExceptionBuilder builder, BitField<SyncOption> options)
    throws ArchiveSyncException {
        controller.sync(builder, options);
    }
}
