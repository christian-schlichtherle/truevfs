/*
 * Copyright (C) 2005-2011 Schlichtherle IT Services
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
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.text.MessageFormat;
import javax.swing.Icon;
import javax.swing.UIManager;

/**
 * A custom file system view required to browse archive files like (virtual)
 * directories with a JFileChooser.
 * This class is used by {@link de.schlichtherle.truezip.file.swing.tree.TFileTreeCellRenderer}
 * to render files and directories in a {@link TFileTree}.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
//
// Unfortunately this is a pretty ugly piece of code.
// The reason for this is the completely broken design of the genuine
// JFileChooser, TFileSystemView, TFileView, ShellFolder and BasicFileChooserUI
// classes.
// The TFileSystemView uses a lot of "instanceof" run time type detections
// in conjunction with Sun's proprietory (and platform dependent) ShellFolder
// class, which subclasses File.
// Other classes like BasicFileChooserUI also rely on the use of the
// ShellFolder class, which they really shouldn't.
// The use of the ShellFolder class is also the sole reason for the existence
// of the file delegate property in de.schlichtherle.truezip.io.TFile.
// For many methods in this class, we need to pass in the delegate to the
// superclass implementation in order for the JFileChooser to work as expected.
//
// Dear Sun: Please enhance the JFileChooser, TFileSystemView, TFileView and
// ShellFolder classes.
// My primary recommendation would be to define clear responsibilities for
// each of the redesigned classes: Most importantly, all (meta) properties of
// a file (like its name, icon, description, etc.) should be clearly located
// in ONE class and whoever uses this should rely on polymorphism rather than
// instanceof conditionals.
// Finally, please put your new design to test with browsing a virtual file
// system (like TrueZIP provides) - the current JFileChooser is just not able
// to do this right.
//
public class TFileSystemView extends TDecoratingFileSystemView {

    private static TFileSystemView defaultView = new TFileSystemView(
            javax.swing.filechooser.FileSystemView.getFileSystemView(),
            null);

    /** Maybe null - uses default then. **/
    private TArchiveDetector archiveDetector;

    private TFileSystemView(
            javax.swing.filechooser.FileSystemView delegate,
            TArchiveDetector archiveDetector) {
        super(delegate);
        this.archiveDetector = archiveDetector;
    }

    public static javax.swing.filechooser.FileSystemView getFileSystemView() {
        return getFileSystemView(null);
    }

    public static javax.swing.filechooser.FileSystemView getFileSystemView(
            TArchiveDetector archiveDetector) {
        return archiveDetector != null
            ? new TFileSystemView(
                javax.swing.filechooser.FileSystemView.getFileSystemView(),
                archiveDetector)
            : defaultView;
    }

    /**
     * Returns a valid archive detector to use with this class.
     * If no archive detector has been explicitly set for this file system
     * view or the archive detector has been set to {@code null},
     * then {@link de.schlichtherle.truezip.file.TFile#getDefaultArchiveDetector} is
     * returned.
     */
    public TArchiveDetector getArchiveDetector() {
        return null != archiveDetector
                ? archiveDetector
                : TFile.getDefaultArchiveDetector();
    }

    /**
     * Sets the archive detector to use within this class.
     *
     * @param archiveDetector The archive detector to use.
     *        May be {@code null} to indicate that
     *        {@link de.schlichtherle.truezip.file.TFile#getDefaultArchiveDetector}
     *        should be used.
     */
    public void setArchiveDetector(TArchiveDetector archiveDetector) {
        this.archiveDetector = archiveDetector;
    }

    /** Wraps the given file in an archive enabled file. */
    protected TFile wrap(final File file) {
        if (file == null)
            return null;
        return file instanceof TFile
                ? (TFile) file
                : new TFile(file, getArchiveDetector());
    }

    /** Unwraps the delegate of a possibly archive enabled file. */
    protected File unwrap(final File file) {
        return file instanceof TFile
                ? ((TFile) file).getDelegate()
                : file;
    }

    /**
     * Creates a ZIP enabled file where necessary only,
     * otherwise the blueprint is simply returned.
     */
    public File createFileObject(final File file) {
        if (file == null)
            return null;
        final TFile wFile = wrap(file);
        return wFile.isArchive() || wFile.isEntry()
                ? wFile
                : unwrap(file);
    }

    //
    // Overridden methods:
    //

    @Override
    public boolean isRoot(File file) {
        return super.isRoot(unwrap(file));
    }

    @Override
    public Boolean isTraversable(File file) {
        final TFile wFile = wrap(file);
        return null != wFile
                ? Boolean.valueOf(wFile.isDirectory())
                : super.isTraversable(unwrap(file));
    }

    @Override
    public String getSystemDisplayName(File file) {
        final TFile wFile = wrap(file);
        if (wFile.isArchive() || wFile.isEntry())
            return wFile.getName();
        return super.getSystemDisplayName(unwrap(file));
    }

    @Override
    public String getSystemTypeDescription(File file) {
        final TFile wFile = wrap(file);
        final String typeDescription = TFileView.typeDescription(wFile);
        if (typeDescription != null)
            return typeDescription;
        return super.getSystemTypeDescription(unwrap(file));
    }

    @Override
    public Icon getSystemIcon(File file) {
        final TFile wFile = wrap(file);
        final Icon icon = TFileView.closedIcon(wFile);
        if (icon != null)
            return icon;
        final File uFile = unwrap(file);
        return uFile.exists()
            ? super.getSystemIcon(uFile)
            : null;
    }

    @Override
    public boolean isParent(File folder, File file) {
        return super.isParent(wrap(folder), wrap(file))
            || super.isParent(unwrap(folder), unwrap(file));
    }

    @Override
    public File getChild(File parent, String child) {
        final TFile wParent = wrap(parent);
        if (wParent.isArchive() || wParent.isEntry())
            return createFileObject(super.getChild(wParent, child));
        return createFileObject(super.getChild(unwrap(parent), child));
    }

    @Override
    public boolean isFileSystem(File file) {
        return super.isFileSystem(unwrap(file));
    }

    @Override
    public File createNewFolder(final File parent)
    throws IOException {
        final TFile wParent = wrap(parent);
        if (wParent.isArchive() || wParent.isEntry()) {
            TFile folder = new TFile(
                    wParent,
                    UIManager.getString(TFile.separatorChar == '\\'
                            ? "FileChooser.win32.newFolder"
                            : "FileChooser.other.newFolder"),
                    getArchiveDetector());

            for (int i = 2; !folder.mkdirs(); i++) {
                if (i > 100)
                    throw new IOException(wParent + ": Could not create new directory entry!");
                folder = new TFile(
                        wParent,
                        MessageFormat.format(
                            UIManager.getString(TFile.separatorChar == '\\'
                                ? "FileChooser.win32.newFolder.subsequent"
                                : "FileChooser.other.newFolder.subsequent"),
                            new Object[] { Integer.valueOf(i) }),
                        getArchiveDetector());
            }

            return folder;
        }
        return createFileObject(super.createNewFolder(unwrap(parent)));
    }

    @Override
    public boolean isHiddenFile(File file) {
        return super.isHiddenFile(unwrap(file));
    }

    @Override
    public boolean isFileSystemRoot(File file) {
        return super.isFileSystemRoot(unwrap(file));
    }

    @Override
    public boolean isDrive(File file) {
        return super.isDrive(unwrap(file));
    }

    @Override
    public boolean isFloppyDrive(File file) {
        return super.isFloppyDrive(unwrap(file));
    }

    @Override
    public boolean isComputerNode(File file) {
        return super.isComputerNode(unwrap(file));
    }

    /**
     * Creates a ZIP enabled file where necessary only,
     * otherwise the file system view delegate is used to create the file.
     */
    @Override
    public File createFileObject(File dir, String str) {
        return createFileObject(super.createFileObject(dir, str));
    }

    /**
     * Creates a ZIP enabled file where necessary only,
     * otherwise the file system view delegate is used to create the file.
     */
    @Override
    public File createFileObject(String str) {
        return createFileObject(super.createFileObject(str));
    }

    @Override
    public File[] getFiles(
            final File dir,
            final boolean useFileHiding) {
        final TFile smartDir = wrap(dir);
        if (smartDir.isArchive() || smartDir.isEntry()) {
            // dir is a ZIP file or an entry in a ZIP file.
            return smartDir.listFiles(new FileFilter() {
                @Override
				public boolean accept(File file) {
                    return !useFileHiding || !isHiddenFile(file);
                }
            });
        } else {
            final File files[] = super.getFiles(unwrap(dir), useFileHiding);
            if (files != null)
                for (int i = files.length; --i >= 0; )
                    files[i] = createFileObject(files[i]);

            return files;
        }
    }

    @Override
    public File getParentDirectory(File file) {
        final TFile wFile = wrap(file);
        if (wFile.isEntry())
            return createFileObject(wFile.getParentFile());
        return createFileObject(super.getParentDirectory(unwrap(file)));
    }

    /*protected File createFileSystemRoot(File file) {
        // As an exception to the rule, we will not delegate this call as this
        // method has protected access.
        // Instead, we will delegate it to our superclass and unwrap the plain
        // file object from it.
        return super.createFileSystemRoot(unwrap(file));
    }*/
}
