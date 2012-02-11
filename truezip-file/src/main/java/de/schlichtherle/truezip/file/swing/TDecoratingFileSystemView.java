/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.file.swing;

import java.io.File;
import java.io.IOException;
import javax.swing.Icon;
import javax.swing.filechooser.FileSystemView;

/**
 * A decorator for a file system view.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public abstract class TDecoratingFileSystemView extends FileSystemView {

    /** The decorated file system view. */
    protected final FileSystemView delegate;

    /**
     * Creates a new decorating file system view.
     *
     * @param fileSystemView the file system view to decorate.
     */
    protected TDecoratingFileSystemView(final FileSystemView fileSystemView) {
        if (null == fileSystemView)
            throw new NullPointerException();
        this.delegate = fileSystemView;
    }

    @Override
    public boolean isRoot(File f) {
        return delegate.isRoot(f);
    }

    @Override
    public Boolean isTraversable(File f) {
        return delegate.isTraversable(f);
    }

    @Override
    public String getSystemDisplayName(File f) {
        return delegate.getSystemDisplayName(f);
    }

    @Override
    public String getSystemTypeDescription(File f) {
        return delegate.getSystemTypeDescription(f);
    }

    @Override
    public Icon getSystemIcon(File f) {
        return delegate.getSystemIcon(f);
    }

    @Override
    public boolean isParent(File folder, File file) {
        return delegate.isParent(folder, file);
    }

    @Override
    public File getChild(File parent, String fileName) {
        return delegate.getChild(parent, fileName);
    }

    @Override
    public boolean isFileSystem(File f) {
        return delegate.isFileSystem(f);
    }

    @Override
    public File createNewFolder(File containingDir) throws IOException {
        return delegate.createNewFolder(containingDir);
    }

    @Override
    public boolean isHiddenFile(File f) {
        return delegate.isHiddenFile(f);
    }

    @Override
    public boolean isFileSystemRoot(File dir) {
        return delegate.isFileSystemRoot(dir);
    }

    @Override
    public boolean isDrive(File dir) {
        return delegate.isDrive(dir);
    }

    @Override
    public boolean isFloppyDrive(File dir) {
        return delegate.isFloppyDrive(dir);
    }

    @Override
    public boolean isComputerNode(File dir) {
        return delegate.isComputerNode(dir);
    }

    @Override
    public File[] getRoots() {
        return delegate.getRoots();
    }

    @Override
    public File getHomeDirectory() {
        return delegate.getHomeDirectory();
    }

    @Override
    public File getDefaultDirectory() {
        return delegate.getDefaultDirectory();
    }

    @Override
    public File createFileObject(File dir, String filename) {
        return delegate.createFileObject(dir, filename);
    }

    @Override
    public File createFileObject(String path) {
        return delegate.createFileObject(path);
    }

    @Override
    public File[] getFiles(File dir, boolean useFileHiding) {
        return delegate.getFiles(dir, useFileHiding);
    }

    @Override
    public File getParentDirectory(File dir) {
        return delegate.getParentDirectory(dir);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link TDecoratingFileSystemView}
     * determines if the delegate is an instance of this class.
     * If this is true, then this method forwards the call to the delegate.
     * Otherwise, the implementation of the super class is called.
     */
    @Override
    protected File createFileSystemRoot(File f) {
        return (delegate instanceof TDecoratingFileSystemView)
                ? ((TDecoratingFileSystemView) delegate).createFileSystemRoot(f)
                : super.createFileSystemRoot(f);
    }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return String.format("%s[delegate=%s]",
                getClass().getName(),
                delegate);
    }
}
