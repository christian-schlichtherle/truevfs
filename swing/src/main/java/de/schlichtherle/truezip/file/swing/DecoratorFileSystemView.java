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
package de.schlichtherle.truezip.file.swing;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.File;
import java.io.IOException;
import javax.swing.filechooser.FileSystemView;
import javax.swing.Icon;

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
abstract class DecoratorFileSystemView extends FileSystemView {

    /** The decorated file system view. */
    @NonNull
    protected final FileSystemView delegate;

    /**
     * Creates a new decorating file system view.
     *
     * @param delegate The file view to be decorated - may be {@code null}.
     */
    protected DecoratorFileSystemView(final FileSystemView delegate) {
        if (delegate == null)
            throw new NullPointerException();
        this.delegate = delegate;
    }

    //
    // Overridden methods:
    //

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

    /** Forwards the call to {@link #createFileSystemRootImpl}. */
    @Override
    protected final File createFileSystemRoot(File f) {
        return createFileSystemRootImpl(f);
    }

    /**
     * Creates a new {@code File} object for {@code f} with correct
     * behavior for a file system root directory.
     * If the delegate is an instance of this class, then this method
     * forwards the call to the delegate like all other methods in this class.
     * Otherwise, the super class implementation of the method
     * {@link javax.swing.filechooser.FileSystemView#createFileSystemRoot(java.io.File)}
     * is called.
     */
    public File createFileSystemRootImpl(File f) {
        return (delegate instanceof DecoratorFileSystemView)
                ? ((DecoratorFileSystemView) delegate).createFileSystemRootImpl(f)
                : super.createFileSystemRoot(f);
    }
}
