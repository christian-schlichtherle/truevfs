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

import java.io.OutputStream;
import de.schlichtherle.truezip.io.socket.FilterOutputSocket;
import de.schlichtherle.truezip.io.rof.ReadOnlyFile;
import java.io.InputStream;
import de.schlichtherle.truezip.io.socket.FilterInputSocket;
import de.schlichtherle.truezip.util.ExceptionBuilder;
import de.schlichtherle.truezip.io.entry.FilterCommonEntry;
import java.util.Set;
import de.schlichtherle.truezip.io.filesystem.FileSystemController;
import de.schlichtherle.truezip.io.archive.driver.ArchiveDriver;
import de.schlichtherle.truezip.io.socket.OutputOption;
import de.schlichtherle.truezip.io.socket.InputOption;
import de.schlichtherle.truezip.io.filesystem.FileSystemEntry;
import de.schlichtherle.truezip.io.entry.CommonEntry;
import de.schlichtherle.truezip.io.entry.CommonEntry.Type;
import de.schlichtherle.truezip.io.entry.CommonEntry.Access;
import de.schlichtherle.truezip.io.socket.OutputSocket;
import de.schlichtherle.truezip.io.socket.InputSocket;
import de.schlichtherle.truezip.util.BitField;
import java.io.IOException;
import javax.swing.Icon;

import static de.schlichtherle.truezip.io.archive.entry.ArchiveEntry.SEPARATOR_CHAR;
import static de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystems.isRoot;
import static de.schlichtherle.truezip.io.Paths.cutTrailingSeparators;
import static de.schlichtherle.truezip.io.entry.CommonEntry.Type.SPECIAL;

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

    /** Returns the file system controller for the enclosing file system. */
    private FileSystemController getEnclController() {
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

    @Override
    public Icon getOpenIcon() {
        try {
            return getController().getOpenIcon();
        } catch (FalsePositiveException ex) {
            if (ex.isTransient())
                return null;
            return getEnclController().getOpenIcon();
        }
    }

    @Override
    public Icon getClosedIcon() {
        try {
            return getController().getClosedIcon();
        } catch (FalsePositiveException ex) {
            if (ex.isTransient())
                return null;
            return getEnclController().getClosedIcon();
        }
    }

    @Override
    public boolean isReadOnly() {
        try {
            return getController().isReadOnly();
        } catch (FalsePositiveException ex) {
            if (ex.isTransient())
                return true;
            return getEnclController().isReadOnly();
        }
    }

    /** @see ArchiveDriver#newInputShop */
    @Override
    public FileSystemEntry getEntry(String path) {
        try {
            return getController().getEntry(path);
        } catch (FalsePositiveException ex) {
            final FileSystemEntry entry = getEnclController()
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
            return getController().isReadable(path);
        } catch (FalsePositiveException ex) {
            if (ex.isTransient())
                return false;
            return getEnclController().isReadable(getEnclPath(path));
        }
    }

    @Override
    public boolean isWritable(String path) {
        try {
            return getController().isWritable(path);
        } catch (FalsePositiveException ex) {
            if (ex.isTransient())
                return false;
            return getEnclController().isWritable(getEnclPath(path));
        }
    }

    @Override
    public void setReadOnly(String path)
    throws IOException {
        try {
            getController().setReadOnly(path);
        } catch (FalsePositiveException ex) {
            if (ex.isTransient())
                throw ex.getCause();
            getEnclController().setReadOnly(getEnclPath(path));
        }
    }

    @Override
    public boolean setTime(String path, BitField<Access> types, long value)
    throws IOException {
        try {
            return getController().setTime(path, types, value);
        } catch (FalsePositiveException ex) {
            if (ex.isTransient())
                throw ex.getCause();
            return getEnclController()
                    .setTime(getEnclPath(path), types, value);
        }
    }

    @Override
    public InputSocket<?> getInputSocket(   String path,
                                            BitField<InputOption> options)
    throws IOException {
        try {
            return new Input(path, options);
        } catch (FalsePositiveException ex) {
            if (ex.isTransient())
                throw ex.getCause();
            return getEnclController()
                    .getInputSocket(getEnclPath(path), options);
        }
    }

    private class Input extends FilterInputSocket<CommonEntry> {
        final String path;
        final BitField<InputOption> options;

        Input(  final String path, final BitField<InputOption> options)
        throws IOException {
            super(getController().getInputSocket(path, options));
            this.path = path;
            this.options = options;
        }

        @Override
        public CommonEntry getLocalTarget() throws IOException {
            try {
                return getInputSocket().getLocalTarget();
            } catch (FalsePositiveException ex) {
                if (ex.isTransient())
                    throw ex.getCause();
                setInputSocket(getEnclController()
                        .getInputSocket(getEnclPath(path), options));
                return getInputSocket().getLocalTarget();
            }
        }

        @Override
        public InputStream newInputStream() throws IOException {
            try {
                return getInputSocket().newInputStream();
            } catch (FalsePositiveException ex) {
                if (ex.isTransient())
                    throw ex.getCause();
                setInputSocket(getEnclController()
                        .getInputSocket(getEnclPath(path), options));
                return getInputSocket().newInputStream();
            }
        }

        @Override
        public ReadOnlyFile newReadOnlyFile() throws IOException {
            try {
                return getInputSocket().newReadOnlyFile();
            } catch (FalsePositiveException ex) {
                if (ex.isTransient())
                    throw ex.getCause();
                setInputSocket(getEnclController()
                        .getInputSocket(getEnclPath(path), options));
                return getInputSocket().newReadOnlyFile();
            }
        }
    } // class Input

    @Override
    public OutputSocket<?> getOutputSocket( String path,
                                            BitField<OutputOption> options)
    throws IOException {
        try {
            return new Output(path, options);
        } catch (FalsePositiveException ex) {
            if (ex.isTransient())
                throw ex.getCause();
            return getEnclController()
                    .getOutputSocket(getEnclPath(path), options);
        }
    }

    private class Output extends FilterOutputSocket<CommonEntry> {
        final String path;
        final BitField<OutputOption> options;

        Output(final String path, final BitField<OutputOption> options)
        throws IOException {
            super(getController().getOutputSocket(path, options));
            this.path = path;
            this.options = options;
        }

        @Override
        public CommonEntry getLocalTarget() throws IOException {
            try {
                return getOutputSocket().getLocalTarget();
            } catch (FalsePositiveException ex) {
                if (ex.isTransient())
                    throw ex.getCause();
                setOutputSocket(getEnclController()
                        .getOutputSocket(getEnclPath(path), options));
                return getOutputSocket().getLocalTarget();
            }
        }

        @Override
        public OutputStream newOutputStream() throws IOException {
            try {
                return getOutputSocket().newOutputStream();
            } catch (FalsePositiveException ex) {
                if (ex.isTransient())
                    throw ex.getCause();
                setOutputSocket(getEnclController()
                        .getOutputSocket(getEnclPath(path), options));
                return getOutputSocket().newOutputStream();
            }
        }
    } // class Output

    @Override
    public boolean mknod(   String path,
                            Type type,
                            CommonEntry template,
                            BitField<OutputOption> options)
    throws IOException {
        try {
            return getController().mknod(path, type, template, options);
        } catch (FalsePositiveException ex) {
            if (ex.isTransient())
                throw ex.getCause();
            return getEnclController()
                    .mknod(getEnclPath(path), type, template, options);
        }
    }

    /** @see ArchiveDriver#newInputShop */
    @Override
    @SuppressWarnings("ThrowableInitCause")
    public void unlink(String path)
    throws IOException {
        try {
            getController().unlink(path);
        } catch (FalsePositiveException ex) {
            if (ex.isTransient())
                throw ex.getCause();
            getEnclController().unlink(getEnclPath(path));
        }
    }

    @Override
    public <E extends IOException>
    void sync(ExceptionBuilder<? super SyncException, E> builder, BitField<SyncOption> options)
    throws E {
        getController().sync(builder, options);
    }
}
