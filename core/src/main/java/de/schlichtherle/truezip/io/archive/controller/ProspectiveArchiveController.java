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

import de.schlichtherle.truezip.io.archive.driver.ArchiveDriver;
import de.schlichtherle.truezip.io.socket.OutputOption;
import de.schlichtherle.truezip.io.socket.InputOption;
import de.schlichtherle.truezip.io.socket.FileSystemEntry;
import de.schlichtherle.truezip.io.socket.CommonEntry;
import de.schlichtherle.truezip.io.socket.CommonEntry.Type;
import de.schlichtherle.truezip.io.socket.CommonEntry.Access;
import de.schlichtherle.truezip.io.socket.FilterCommonEntry;
import de.schlichtherle.truezip.io.socket.OutputSocket;
import de.schlichtherle.truezip.io.socket.InputSocket;
import de.schlichtherle.truezip.util.BitField;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Set;
import javax.swing.Icon;

import static de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystems.isRoot;
import static de.schlichtherle.truezip.io.socket.CommonEntry.Type.FILE;
import static de.schlichtherle.truezip.io.socket.CommonEntry.Type.SPECIAL;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
final class ProspectiveArchiveController extends ArchiveController {

    private final ArchiveController controller;

    ProspectiveArchiveController(   final ArchiveModel model,
                                    final ArchiveController controller) {
        super(model);
        assert null != controller;
        this.controller = controller;
    }

    private ArchiveController getController() {
        return controller;
    }

    @Override
    public Icon getOpenIcon() {
        try {
            return getController().getOpenIcon();
        } catch (FalsePositiveEntryException ex) {
            return getEnclController().getOpenIcon();
        }
    }

    @Override
    public Icon getClosedIcon() {
        try {
            return getController().getClosedIcon();
        } catch (FalsePositiveEntryException ex) {
            return getEnclController().getClosedIcon();
        }
    }

    @Override
    public boolean isReadOnly() {
        try {
            return getController().isReadOnly();
        } catch (FalsePositiveEntryException ex) {
            return getEnclController().isReadOnly();
        }
    }

    /** @see ArchiveDriver#newInputShop! */
    @Override
    public FileSystemEntry getEntry(String path) {
        try {
            return getController().getEntry(path);
        } catch (FalsePositiveEntryException ex) {
            final FileSystemEntry entry
                    = getEnclController().getEntry(getEnclPath(path));
            if (isRoot(path)) {
                if (null != entry && FILE == entry.getType()
                && ex.getCause() instanceof FileNotFoundException)
                    return new SpecialFileEntry(entry);
            }
            return entry;
        }
    }

    private static final class SpecialFileEntry
    extends FilterCommonEntry<FileSystemEntry>
    implements FileSystemEntry {
        SpecialFileEntry(FileSystemEntry entry) {
            super(entry);
            assert FILE == entry.getType();
        }

        @Override
        public Type getType() {
            return SPECIAL;
        }

        @Override
        public Set<String> getMembers() {
            return null;
        }
    }

    @Override
    public boolean isReadable(String path) {
        try {
            return getController().isReadable(path);
        } catch (FalsePositiveEntryException ex) {
            return getEnclController().isReadable(getEnclPath(path));
        }
    }

    @Override
    public boolean isWritable(String path) {
        try {
            return getController().isWritable(path);
        } catch (FalsePositiveEntryException ex) {
            return getEnclController().isWritable(getEnclPath(path));
        }
    }

    @Override
    public void setReadOnly(String path)
    throws IOException {
        try {
            getController().setReadOnly(path);
        } catch (FalsePositiveEntryException ex) {
            getEnclController().setReadOnly(getEnclPath(path));
        }
    }

    @Override
    public boolean setTime(String path, BitField<Access> types, long value)
    throws IOException {
        try {
            return getController().setTime(path, types, value);
        } catch (FalsePositiveEntryException ex) {
            return getEnclController().setTime(getEnclPath(path), types, value);
        }
    }

    @Override
    public InputSocket<?> newInputSocket(
            String path,
            BitField<InputOption> options)
    throws IOException {
        // TODO: Return a custom socket which supports lazy false positive
        // detection when a stream is created - see LockingArchiveController.
        try {
            return getController().newInputSocket(path, options);
        } catch (FalsePositiveEntryException ex) {
            return getEnclController().newInputSocket(getEnclPath(path), options);
        }
    }

    @Override
    public OutputSocket<?> newOutputSocket(
            String path,
            BitField<OutputOption> options)
    throws IOException {
        // TODO: Return a custom socket which supports lazy false positive
        // detection when a stream is created - see LockingArchiveController.
        try {
            return getController().newOutputSocket(path, options);
        } catch (FalsePositiveEntryException ex) {
            return getEnclController().newOutputSocket(getEnclPath(path), options);
        }
    }

    @Override
    public boolean mknod(   String path,
                            Type type,
                            CommonEntry template,
                            BitField<OutputOption> options)
    throws IOException {
        try {
            return getController().mknod(path, type, template, options);
        } catch (FalsePositiveEntryException ex) {
            return getEnclController().mknod(getEnclPath(path), type, template, options);
        }
    }

    /** @see ArchiveDriver#newInputShop! */
    @Override
    @SuppressWarnings("ThrowableInitCause")
    public void unlink(String path)
    throws IOException {
        try {
            getController().unlink(path);
            return;
        } catch (FalsePositiveEntryException ex) {
            // FIXME: Check if needed anymore!
            // What if we remove this special case? We could probably delete a RAES encrypted ZIP file with an unknown password. Would we want this?
            if (isRoot(path)) {
                final FileSystemEntry entry = getEnclController().getEntry(getEnclPath(path));
                if (null != entry && FILE == entry.getType()
                && ex.getCause() instanceof FileNotFoundException) {
                    throw (IOException) new IOException(ex.toString()).initCause(ex); // mask!
                }
            }
            // Fall through!
        }
        getEnclController().unlink(getEnclPath(path));
    }

    @Override
    public void sync(   ArchiveSyncExceptionBuilder builder,
                        BitField<SyncOption> options)
    throws ArchiveSyncException {
        getController().sync(builder, options);
    }
}
