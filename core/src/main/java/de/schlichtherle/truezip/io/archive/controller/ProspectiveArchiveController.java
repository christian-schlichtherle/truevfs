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

import de.schlichtherle.truezip.util.ExceptionBuilder;
import de.schlichtherle.truezip.io.socket.FilterCommonEntry;
import java.util.Set;
import de.schlichtherle.truezip.io.filesystem.FileSystemController;
import de.schlichtherle.truezip.io.archive.driver.ArchiveDriver;
import de.schlichtherle.truezip.io.socket.OutputOption;
import de.schlichtherle.truezip.io.socket.InputOption;
import de.schlichtherle.truezip.io.socket.FileSystemEntry;
import de.schlichtherle.truezip.io.socket.CommonEntry;
import de.schlichtherle.truezip.io.socket.CommonEntry.Type;
import de.schlichtherle.truezip.io.socket.CommonEntry.Access;
import de.schlichtherle.truezip.io.socket.OutputSocket;
import de.schlichtherle.truezip.io.socket.InputSocket;
import de.schlichtherle.truezip.util.BitField;
import java.io.IOException;
import javax.swing.Icon;

import static de.schlichtherle.truezip.io.archive.entry.ArchiveEntry.SEPARATOR_CHAR;
import static de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystems.isRoot;
import static de.schlichtherle.truezip.io.Paths.cutTrailingSeparators;
import static de.schlichtherle.truezip.io.socket.CommonEntry.Type.SPECIAL;

/**
 * Deals with {@link FalsePositiveException}.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
final class ProspectiveArchiveController extends FilterArchiveController {

    private final FileSystemController enclController;
    private final String enclPath;

    ProspectiveArchiveController(   final FileSystemController enclController,
                                    final ArchiveController controller) {
        super(controller);
        this.enclController = enclController;
        this.enclPath = enclController
                .getModel()
                .getMountPoint()
                .relativize(controller.getModel().getMountPoint())
                .getPath();
    }

    private String getPath(String path) {
        return path;
    }

    /** Returns the file system controller for the enclosing file system. */
    private FileSystemController getEnclController(FalsePositiveException ex) {
        return enclController;
    }

    /**
     * Resolves the given relative {@code path} against the relative path of
     * this controller's archive file within its enclosing file system.
     */
    private String getEnclPath(String path) {
        return isRoot(path)
                ? cutTrailingSeparators(enclPath, SEPARATOR_CHAR)
                : enclPath + path;
    }

    private void reset() {
    }

    @Override
    public Icon getOpenIcon() {
        try {
            return getController().getOpenIcon();
        } catch (FalsePositiveException ex) {
            if (ex.isTransient())
                return null;
            return getEnclController(ex).getOpenIcon();
        }
    }

    @Override
    public Icon getClosedIcon() {
        try {
            return getController().getClosedIcon();
        } catch (FalsePositiveException ex) {
            if (ex.isTransient())
                return null;
            return getEnclController(ex).getClosedIcon();
        }
    }

    @Override
    public boolean isReadOnly() {
        try {
            return getController().isReadOnly();
        } catch (FalsePositiveException ex) {
            if (ex.isTransient())
                return true;
            return getEnclController(ex).isReadOnly();
        }
    }

    /** @see ArchiveDriver#newInputShop! */
    @Override
    public FileSystemEntry getEntry(String path) {
        try {
            return getController().getEntry(getPath(path));
        } catch (FalsePositiveException ex) {
            final FileSystemEntry entry = getEnclController(ex)
                    .getEntry(getEnclPath(path));
            if (ex.isTransient())
                return null == entry ? null : new SpecialFileEntry(entry);
            return entry;
        }
    }

    private static final class SpecialFileEntry
    extends FilterCommonEntry<FileSystemEntry>
    implements FileSystemEntry {
        SpecialFileEntry(FileSystemEntry entry) {
            super(entry);
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
            return getController().isReadable(getPath(path));
        } catch (FalsePositiveException ex) {
            if (ex.isTransient())
                return false;
            return getEnclController(ex).isReadable(getEnclPath(path));
        }
    }

    @Override
    public boolean isWritable(String path) {
        try {
            return getController().isWritable(getPath(path));
        } catch (FalsePositiveException ex) {
            if (ex.isTransient())
                return false;
            return getEnclController(ex).isWritable(getEnclPath(path));
        }
    }

    @Override
    public void setReadOnly(String path)
    throws IOException {
        try {
            getController().setReadOnly(getPath(path));
        } catch (FalsePositiveException ex) {
            if (ex.isTransient())
                throw ex.getCause();
            getEnclController(ex).setReadOnly(getEnclPath(path));
        }
    }

    @Override
    public boolean setTime(String path, BitField<Access> types, long value)
    throws IOException {
        try {
            return getController().setTime(getPath(path), types, value);
        } catch (FalsePositiveException ex) {
            if (ex.isTransient())
                throw ex.getCause();
            return getEnclController(ex)
                    .setTime(getEnclPath(path), types, value);
        }
    }

    @Override
    public InputSocket<?> getInputSocket(
            String path,
            BitField<InputOption> options)
    throws IOException {
        // TODO: Return a custom socket which supports lazy false positive
        // detection when a stream is created - see LockingArchiveController.
        try {
            return getController().getInputSocket(getPath(path), options);
        } catch (FalsePositiveException ex) {
            if (ex.isTransient())
                throw ex.getCause();
            return getEnclController(ex)
                    .getInputSocket(getEnclPath(path), options);
        }
    }

    @Override
    public OutputSocket<?> getOutputSocket(
            String path,
            BitField<OutputOption> options)
    throws IOException {
        // TODO: Return a custom socket which supports lazy false positive
        // detection when a stream is created - see LockingArchiveController.
        try {
            return getController().getOutputSocket(getPath(path), options);
        } catch (FalsePositiveException ex) {
            if (ex.isTransient())
                throw ex.getCause();
            return getEnclController(ex)
                    .getOutputSocket(getEnclPath(path), options);
        }
    }

    @Override
    public boolean mknod(   String path,
                            Type type,
                            CommonEntry template,
                            BitField<OutputOption> options)
    throws IOException {
        try {
            return getController().mknod(getPath(path), type, template, options);
        } catch (FalsePositiveException ex) {
            if (ex.isTransient())
                throw ex.getCause();
            return getEnclController(ex)
                    .mknod(getEnclPath(path), type, template, options);
        }
    }

    /** @see ArchiveDriver#newInputShop! */
    @Override
    @SuppressWarnings("ThrowableInitCause")
    public void unlink(String path)
    throws IOException {
        try {
            getController().unlink(getPath(path));
        } catch (FalsePositiveException ex) {
            if (ex.isTransient())
                throw ex.getCause();
            getEnclController(ex).unlink(getEnclPath(path));
        }
    }

    @Override
    public <E extends IOException>
    void sync(ExceptionBuilder<? super SyncException, E> builder, BitField<SyncOption> options)
    throws E {
        reset();
        super.getController().sync(builder, options);
    }
}
