/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.file.swing;

import net.truevfs.file.TFile;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileSystemView;

/**
 * A custom {@link JFileChooser} which supports browsing archive files
 * like (virtual) directories.
 *
 * @author Christian Schlichtherle
 */
public final class TFileChooser extends JFileChooser {
    private static final long serialVersionUID = 936528972682036204L;

    public TFileChooser() {
        this(null, null);
    }

    public TFileChooser(@CheckForNull TFile currentDirectory) {
        this(currentDirectory, null);
    }

    public TFileChooser(@CheckForNull TFileSystemView fileSystemView) {
        this(null, fileSystemView);
    }

    public TFileChooser(@CheckForNull TFile currentDirectory,
                        @CheckForNull TFileSystemView fileSystemView) {
        super(  currentDirectory,
                null != fileSystemView ? fileSystemView : new TFileSystemView());
        super.setFileView(new TFileView(super.getFileView()));
        //super.setDoubleBuffered(false);
    }

    private void readObject(ObjectInputStream in)
    throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        super.setFileSystemView(new TFileSystemView());
    }

    @Override
    public TFile getCurrentDirectory() {
        return getFileSystemView().wrap(super.getCurrentDirectory());
    }

    @Override
    public TFileSystemView getFileSystemView() {
        return (TFileSystemView) super.getFileSystemView();
    }

    /**
     * {@inheritDoc}
     * <p>
     * @throws ClassCastException if {@code fileSystemView} is not an
     *         instance of {@link TFileSystemView}.
     */
    @Override
    public void setFileSystemView(FileSystemView fileSystemView) {
        super.setFileSystemView(
                (TFileSystemView) Objects.requireNonNull(fileSystemView));
    }

    @Override
    public @Nullable TFile getSelectedFile() {
        return getFileSystemView().wrap(super.getSelectedFile());
    }

    @Override
    public @Nullable TFile[] getSelectedFiles() {
        final File files[] = super.getSelectedFiles();
        if (null == files)
            return null; // no directory
        final TFileSystemView fsv = getFileSystemView();
        final TFile[] results = new TFile[files.length];
        for (int i = files.length; 0 <= --i; )
            results[i] = fsv.wrap(files[i]);
        return results;
    }
}
