/*
 * Copyright (C) 2005-2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.file.swing;

import de.schlichtherle.truezip.file.TFile;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileSystemView;

/**
 * A custom {@link JFileChooser} which supports browsing archive files
 * like (virtual) directories.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
public final class TFileChooser extends JFileChooser {
    private static final long serialVersionUID = 936528972682036204L;

    //private transient volatile short paintingDisabled; // = false;

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
        if (null == fileSystemView)
            throw new NullPointerException();
        super.setFileSystemView((TFileSystemView) fileSystemView);
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
