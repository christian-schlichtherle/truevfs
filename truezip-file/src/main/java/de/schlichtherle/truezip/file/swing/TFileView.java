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

import de.schlichtherle.truezip.file.TArchiveDetector;
import de.schlichtherle.truezip.file.TFile;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
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
@DefaultAnnotation(NonNull.class)
public class TFileView extends TDecoratingFileView {

    private static final String CLASS_NAME = TFileView.class.getName();
    private static final ResourceBundle
            resources = ResourceBundle.getBundle(CLASS_NAME);

    /**
     * Creates a new archive enabled file view.
     *
     * @param fileView The nullable file view to decorate.
     */
    public TFileView(@CheckForNull FileView fileView) {
        super(null != fileView ? fileView : new TNullFileView());
    }

    @Override
    public @Nullable String getTypeDescription(File file) {
        String typeDescription = typeDescription(file);
        return typeDescription != null
                ? typeDescription
                : delegate.getTypeDescription(file);
    }

    static @Nullable String typeDescription(File file) {
        if (!(file instanceof TFile))
            return null;
        TFile smartFile = (TFile) file;
        if (isTrueArchive(smartFile)) {
            return resources.getString("archiveFile");
        } else if (isEntryInTrueArchive(smartFile)) {
            return smartFile.isDirectory()
                    ? resources.getString("archiveDirectoryEntry")
                    : resources.getString("archiveFileEntry");
        }
        return null;
    }

    @Override
    public @Nullable Icon getIcon(File file) {
        Icon icon = icon(file);
        return null != icon ? icon : delegate.getIcon(file);
    }

    static @Nullable Icon icon(File file) {
        if (!(file instanceof TFile))
            return null;
        TFile smartFile = (TFile) file;
        if (isTrueArchive(smartFile)) {
                return UIManager.getIcon("FileView.directoryIcon");
        } else if (isEntryInTrueArchive(smartFile)) {
            return smartFile.isDirectory()
                    ? UIManager.getIcon("FileView.directoryIcon")
                    : UIManager.getIcon("FileView.fileIcon");
        }
        return null;
    }

    private static boolean isTrueArchive(TFile file) {
        return file.isArchive() && file.isDirectory()
                && !newNonArchiveFile(file).isDirectory();
    }

    private static TFile newNonArchiveFile(@NonNull TFile file) {
        TFile parent = file.getParentFile();
        assert null != parent : "expected non-null from context!";
        return new TFile(parent, file.getName(), TArchiveDetector.NULL);
    }

    private static boolean isEntryInTrueArchive(TFile file) {
        // An archive entry always names a parent. This parent must not be
        // a regular directory.
        if (!file.isEntry())
            return false;
        TFile parent = file.getParentFile();
        assert parent != null : "An archive entry must always name a parent!";
        return parent.isDirectory()
                && !new TFile(parent.getPath(), TArchiveDetector.NULL)
                    .isDirectory();
    }

    @Override
    public @Nullable Boolean isTraversable(File file) {
        return file instanceof TFile
                ? Boolean.valueOf(((TFile) file).isDirectory())
                : delegate.isTraversable(file);
    }
}
