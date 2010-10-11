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
import java.io.IOException;
import java.util.Set;
import javax.swing.Icon;

import static de.schlichtherle.truezip.io.archive.entry.ArchiveEntry.SEPARATOR_CHAR;
import static de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystems.isRoot;
import static de.schlichtherle.truezip.io.Paths.cutTrailingSeparators;
import static de.schlichtherle.truezip.io.socket.CommonEntry.Type.SPECIAL;
import static de.schlichtherle.truezip.io.socket.CommonEntry.ROOT;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
final class ProspectiveArchiveController extends ArchiveController {

    private final ArchiveController controller;
    private FileSystemController fsc;
    private String p;

    ProspectiveArchiveController(   final ArchiveModel model,
                                    final ArchiveController controller) {
        super(model);
        assert null != controller;
        this.controller = controller;
        reset();
    }

    private void reset() {
        fsc = controller;
        p = ROOT;
    }

    private FileSystemController getController() {
        return fsc;
    }

    private String getPath(String path) {
        return isRoot(path)
                ? cutTrailingSeparators(p, SEPARATOR_CHAR)
                : p + path;
    }

    /*@Override
    protected FileSystemController getEnclController() {
        if (fsc instanceof ArchiveController) {
            final ArchiveController ac = ((ArchiveController) fsc);
            fsc = ac.getEnclController();
            p = ac.getEnclPath(p);
        }
        return fsc;
    }

    @Override
    protected String getEnclPath(String path) {
        return getPath(path);
    }*/

    @Override
    public Icon getOpenIcon() {
        try {
            return getController().getOpenIcon();
        } catch (FalsePositiveEntryException ex) {
            if (!ex.isTransient())
                return null;
            return getEnclController().getOpenIcon();
        }
    }

    @Override
    public Icon getClosedIcon() {
        try {
            return getController().getClosedIcon();
        } catch (FalsePositiveEntryException ex) {
            if (!ex.isTransient())
                return null;
            return getEnclController().getClosedIcon();
        }
    }

    @Override
    public boolean isReadOnly() {
        try {
            return getController().isReadOnly();
        } catch (FalsePositiveEntryException ex) {
            if (!ex.isTransient())
                return true;
            return getEnclController().isReadOnly();
        }
    }

    /** @see ArchiveDriver#newInputShop! */
    @Override
    public FileSystemEntry getEntry(String path) {
        try {
            return getController().getEntry(getPath(path));
        } catch (FalsePositiveEntryException ex) {
            final FileSystemEntry entry
                    = getEnclController().getEntry(getEnclPath(path));
            /*if (ex.isTransient() && ex.getCause() instanceof FileNotFoundException)
                return null == entry ? null : new SpecialFileEntry(entry);*/
            if (!ex.isTransient())
                return null;
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
        } catch (FalsePositiveEntryException ex) {
            if (!ex.isTransient())
                return false;
            return getEnclController().isReadable(getEnclPath(path));
        }
    }

    @Override
    public boolean isWritable(String path) {
        try {
            return getController().isWritable(getPath(path));
        } catch (FalsePositiveEntryException ex) {
            if (!ex.isTransient())
                return false;
            return getEnclController().isWritable(getEnclPath(path));
        }
    }

    @Override
    public void setReadOnly(String path)
    throws IOException {
        try {
            getController().setReadOnly(getPath(path));
        } catch (FalsePositiveEntryException ex) {
            if (!ex.isTransient())
                throw ex.getCause();
            getEnclController().setReadOnly(getEnclPath(path));
        }
    }

    @Override
    public boolean setTime(String path, BitField<Access> types, long value)
    throws IOException {
        try {
            return getController().setTime(getPath(path), types, value);
        } catch (FalsePositiveEntryException ex) {
            if (!ex.isTransient())
                throw ex.getCause();
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
            return getController().newInputSocket(getPath(path), options);
        } catch (FalsePositiveEntryException ex) {
            if (!ex.isTransient())
                throw ex.getCause();
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
            return getController().newOutputSocket(getPath(path), options);
        } catch (FalsePositiveEntryException ex) {
            if (!ex.isTransient())
                throw ex.getCause();
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
            return getController().mknod(getPath(path), type, template, options);
        } catch (FalsePositiveEntryException ex) {
            if (!ex.isTransient())
                throw ex.getCause();
            return getEnclController().mknod(getEnclPath(path), type, template, options);
        }
    }

    @Override
    @SuppressWarnings("ThrowableInitCause")
    public void unlink(String path)
    throws IOException {
        try {
            getController().unlink(getPath(path));
        } catch (FalsePositiveEntryException ex) {
            if (!ex.isTransient())
                throw ex.getCause();
            getEnclController().unlink(getEnclPath(path));
        }
    }

    @Override
    public void sync(   ArchiveSyncExceptionBuilder builder,
                        BitField<SyncOption> options)
    throws ArchiveSyncException {
        reset();
        getController().sync(builder, options);
    }
}
