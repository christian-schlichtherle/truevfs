/*
 * Copyright (C) 2006-2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.file.swing;

import de.schlichtherle.truezip.file.TDefaultArchiveDetector;
import de.schlichtherle.truezip.file.TFile;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.File;
import java.util.ResourceBundle;
import javax.swing.Icon;
import javax.swing.UIManager;
import javax.swing.filechooser.FileView;

/**
 * An archive enabled file view.
 * This class recognizes instances of the {@link TFile} class and returns
 * custom icons and type descriptions if it's an archive file or an archive
 * entry.
 * Otherwise, the super class behaviour is used.
 * <p>
 * Note that this class accesses archive files lazily, i.e. it does not
 * eagerly check the true status with {@link TFile#isDirectory} or similar
 * unless really necessary. This is to prevent dead locks between the Event
 * Dispatch Thread and the Basic L&F TFile Loading Threads which are forked
 * by JFileChooser.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
final class TFileView extends TDecoratingFileView {

    private static final String CLASS_NAME = TFileView.class.getName();
    private static final ResourceBundle resources
            = ResourceBundle.getBundle(CLASS_NAME);

    /**
     * Creates a new archive enabled file view.
     *
     * @param delegate The file view to be decorated - may be {@code null}.
     */
    public TFileView(FileView delegate) {
        super(delegate);
    }

    @Override
    public Icon getIcon(File file) {
        Icon icon = closedIcon(file);
        return icon != null
                ? icon
                : super.getIcon(file);
    }

    @Override
    public String getTypeDescription(File file) {
        String typeDescription = typeDescription(file);
        return typeDescription != null
                ? typeDescription
                : super.getTypeDescription(file);
    }

    @Override
    public Boolean isTraversable(File file) {
        return file instanceof TFile
                ? Boolean.valueOf(((TFile) file).isDirectory())
                : super.isTraversable(file);
    }

    static Icon openIcon(File file) {
        return icon(file);
    }

    static Icon closedIcon(File file) {
        return icon(file);
    }

    private static Icon icon(File file) {
        if (!(file instanceof TFile))
            return null;
        TFile smartFile = (TFile) file;
        if (isValidArchive(smartFile)) {
                return UIManager.getIcon("FileView.directoryIcon");
        } else if (isEntryInValidArchive(smartFile)) {
            return smartFile.isDirectory()
                    ? UIManager.getIcon("FileView.directoryIcon")
                    : UIManager.getIcon("FileView.fileIcon");
        }
        return null;
    }

    static String typeDescription(File file) {
        if (!(file instanceof TFile))
            return null;
        TFile smartFile = (TFile) file;
        if (isValidArchive(smartFile)) {
            return resources.getString("archiveFile");
        } else if (isEntryInValidArchive(smartFile)) {
            return smartFile.isDirectory()
                    ? resources.getString("archiveDirectoryEntry")
                    : resources.getString("archiveFileEntry");
        }
        return null;
    }

    private static boolean isValidArchive(TFile file) {
        return file.isArchive() && file.isDirectory()
                && !newNonArchiveFile(file).isDirectory();
    }

    private static TFile newNonArchiveFile(@NonNull TFile file) {
        final TFile parent = file.getParentFile();
        assert null != parent : "expected non-null from context!";
        return new TFile(parent, file.getName(), TDefaultArchiveDetector.NULL);
    }

    private static boolean isEntryInValidArchive(TFile file) {
        // An archive entry always names a parent. This parent must not be
        // a regular directory.
        if (!file.isEntry())
            return false;
        File parent = file.getParentFile();
        assert parent != null : "An archive entry must always name a parent!";
        return parent.isDirectory()
                && !new TFile(parent.getPath(), TDefaultArchiveDetector.NULL)
                    .isDirectory();
    }
}
