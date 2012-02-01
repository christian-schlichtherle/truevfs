/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.file.swing;

import de.schlichtherle.truezip.file.TFile;
import edu.umd.cs.findbugs.annotations.CheckForNull;
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
        return file instanceof TFile ? typeDescription((TFile) file) : null;
    }

    private static @Nullable String typeDescription(TFile file) {
        if (isTrueArchive(file)) {
            return resources.getString("archiveFile");
        } else if (isTrueEntry(file)) {
            return file.isDirectory()
                    ? resources.getString("archiveDirectoryEntry")
                    : file.isFile()
                        ? resources.getString("archiveFileEntry")
                        : null;
        }
        return null;
    }

    @Override
    public @Nullable Icon getIcon(File file) {
        Icon icon = icon(file);
        return null != icon ? icon : delegate.getIcon(file);
    }

    static @Nullable Icon icon(File file) {
        return file instanceof TFile ? icon((TFile) file) : null;
    }

    private static @Nullable Icon icon(final TFile file) {
        if (isTrueArchive(file)) {
            return UIManager.getIcon("FileView.directoryIcon");
        } else if (isTrueEntry(file)) {
            return file.isDirectory()
                    ? UIManager.getIcon("FileView.directoryIcon")
                    : file.isFile()
                        ? UIManager.getIcon("FileView.fileIcon")
                        : null;
        }
        return null;
    }

    private static boolean isTrueArchive(TFile file) {
        return file.isArchive() && file.isDirectory();
    }

    private static boolean isTrueEntry(TFile file) {
        //return file.isEntry() && file.getParentFile().isDirectory();
        // An archive entry always names a parent. This parent must not be
        // a regular directory.
        if (!file.isEntry())
            return false;
        TFile parent = file.getParentFile();
        assert parent != null : "An archive entry must always name a parent!";
        return parent.isDirectory();
    }

    @Override
    public @Nullable Boolean isTraversable(File file) {
        return file instanceof TFile
                ? Boolean.valueOf(((TFile) file).isDirectory())
                : delegate.isTraversable(file);
    }
}
