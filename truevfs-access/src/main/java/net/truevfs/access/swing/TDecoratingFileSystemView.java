/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.access.swing;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import javax.swing.Icon;
import javax.swing.filechooser.FileSystemView;

/**
 * A decorator for a file system view.
 *
 * @author Christian Schlichtherle
 */
public abstract class TDecoratingFileSystemView extends FileSystemView {

    /** The decorated file system view. */
    protected final FileSystemView fsv;

    /**
     * Creates a new decorating file system view.
     *
     * @param fsv the file system view to decorate.
     */
    protected TDecoratingFileSystemView(final FileSystemView fsv) {
        this.fsv = Objects.requireNonNull(fsv);
    }

    @Override
    public boolean isRoot(File f) {
        return fsv.isRoot(f);
    }

    @Override
    public Boolean isTraversable(File f) {
        return fsv.isTraversable(f);
    }

    @Override
    public String getSystemDisplayName(File f) {
        return fsv.getSystemDisplayName(f);
    }

    @Override
    public String getSystemTypeDescription(File f) {
        return fsv.getSystemTypeDescription(f);
    }

    @Override
    public Icon getSystemIcon(File f) {
        return fsv.getSystemIcon(f);
    }

    @Override
    public boolean isParent(File folder, File file) {
        return fsv.isParent(folder, file);
    }

    @Override
    public File getChild(File parent, String fileName) {
        return fsv.getChild(parent, fileName);
    }

    @Override
    public boolean isFileSystem(File f) {
        return fsv.isFileSystem(f);
    }

    @Override
    public File createNewFolder(File containingDir) throws IOException {
        return fsv.createNewFolder(containingDir);
    }

    @Override
    public boolean isHiddenFile(File f) {
        return fsv.isHiddenFile(f);
    }

    @Override
    public boolean isFileSystemRoot(File dir) {
        return fsv.isFileSystemRoot(dir);
    }

    @Override
    public boolean isDrive(File dir) {
        return fsv.isDrive(dir);
    }

    @Override
    public boolean isFloppyDrive(File dir) {
        return fsv.isFloppyDrive(dir);
    }

    @Override
    public boolean isComputerNode(File dir) {
        return fsv.isComputerNode(dir);
    }

    @Override
    public File[] getRoots() {
        return fsv.getRoots();
    }

    @Override
    public File getHomeDirectory() {
        return fsv.getHomeDirectory();
    }

    @Override
    public File getDefaultDirectory() {
        return fsv.getDefaultDirectory();
    }

    @Override
    public File createFileObject(File dir, String filename) {
        return fsv.createFileObject(dir, filename);
    }

    @Override
    public File createFileObject(String path) {
        return fsv.createFileObject(path);
    }

    @Override
    public File[] getFiles(File dir, boolean useFileHiding) {
        return fsv.getFiles(dir, useFileHiding);
    }

    @Override
    public File getParentDirectory(File dir) {
        return fsv.getParentDirectory(dir);
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
        return (fsv instanceof TDecoratingFileSystemView)
                ? ((TDecoratingFileSystemView) fsv).createFileSystemRoot(f)
                : super.createFileSystemRoot(f);
    }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return String.format("%s[fsv=%s]",
                getClass().getName(),
                fsv);
    }
}
