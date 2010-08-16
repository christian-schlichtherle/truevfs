/*
 * Copyright (C) 2006-2010 Schlichtherle IT Services
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

package de.schlichtherle.io.swing;

import java.io.*;
import javax.swing.*;

import javax.swing.filechooser.FileSystemView;

/**
 * An abstract decorator which simply delegates to another instance of
 * {@link FileSystemView}.
 * Subclasses should override individual methods to implement specific
 * behaviour.
 * Note that this class does not override any methods in {@link Object}
 * - this should be done in subclasses.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
abstract class FilterFileSystemView extends FileSystemView {

    /** The file system view to be decorated - never <code>null</code>. */
    private FileSystemView delegate;
    
    /**
     * Creates a new decorating file system view.
     *
     * @param delegate The file view to be decorated - may be <code>null</code>.
     */
    protected FilterFileSystemView(final FileSystemView delegate) {
        if (delegate == null)
            throw new NullPointerException();
        this.delegate = delegate;
    }

    /**
     * Returns the file system view to be decorated - never <code>null</code>.
     */
    public FileSystemView getDelegate() {
        return delegate;
    }

    /**
     * Sets the file system view to be decorated.
     *
     * @throws NullPointerException If <code>delegate</code> is <code>null</code>.
     * @throws IllegalArgumentException If <code>delegate</code> is this
     *         instance.
     */
    public void setDelegate(final FileSystemView delegate) {
        if (delegate == null)
            throw new NullPointerException();
        if (delegate == this)
            throw new IllegalArgumentException();
        this.delegate = delegate;
    }

    //
    // Overridden methods:
    //

    public boolean isRoot(File f) {
        return delegate.isRoot(f);
    }

    public Boolean isTraversable(File f) {
        return delegate.isTraversable(f);
    }

    public String getSystemDisplayName(File f) {
        return delegate.getSystemDisplayName(f);
    }

    public String getSystemTypeDescription(File f) {
        return delegate.getSystemTypeDescription(f);
    }

    public Icon getSystemIcon(File f) {
        return delegate.getSystemIcon(f);
    }

    public boolean isParent(File folder, File file) {
        return delegate.isParent(folder, file);
    }

    public File getChild(File parent, String fileName) {
        return delegate.getChild(parent, fileName);
    }

    public boolean isFileSystem(File f) {
        return delegate.isFileSystem(f);
    }

    public File createNewFolder(File containingDir) throws IOException {
        return delegate.createNewFolder(containingDir);
    }

    public boolean isHiddenFile(File f) {
        return delegate.isHiddenFile(f);
    }

    public boolean isFileSystemRoot(File dir) {
        return delegate.isFileSystemRoot(dir);
    }

    public boolean isDrive(File dir) {
        return delegate.isDrive(dir);
    }

    public boolean isFloppyDrive(File dir) {
        return delegate.isFloppyDrive(dir);
    }

    public boolean isComputerNode(File dir) {
        return delegate.isComputerNode(dir);
    }

    public File[] getRoots() {
        return delegate.getRoots();
    }
    
    public File getHomeDirectory() {
        return delegate.getHomeDirectory();
    }

    public File getDefaultDirectory() {
        return delegate.getDefaultDirectory();
    }
    
    public File createFileObject(File dir, String filename) {
        return delegate.createFileObject(dir, filename);
    }

    public File createFileObject(String path) {
        return delegate.createFileObject(path);
    }

    public File[] getFiles(File dir, boolean useFileHiding) {
        return delegate.getFiles(dir, useFileHiding);
    }

    public File getParentDirectory(File dir) {
        return delegate.getParentDirectory(dir);
    }

    /** Forwards the call to {@link #createFileSystemRootImpl}. */
    protected final File createFileSystemRoot(File f) {
        return createFileSystemRootImpl(f);
    }

    /**
     * Creates a new <code>File</code> object for <code>f</code> with correct
     * behavior for a file system root directory.
     * If the delegate is an instance of this class, then this method
     * forwards the call to the delegate like all other methods in this class.
     * Otherwise, the super class implementation of the method
     * {@link javax.swing.filechooser.FileSystemView#createFileSystemRoot(java.io.File)}
     * is called.
     */
    public File createFileSystemRootImpl(File f) {
        return (delegate instanceof FilterFileSystemView)
                ? ((FilterFileSystemView) delegate).createFileSystemRootImpl(f)  
                : super.createFileSystemRoot(f);
    }
}
