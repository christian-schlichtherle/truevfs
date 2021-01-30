/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.key.swing.io;

import net.java.truecommons3.key.swing.util.AbstractComboBoxBrowser;

import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.io.FilenameFilter;
import java.text.Collator;
import java.util.Arrays;
import java.util.Objects;

/**
 * Subclasses {@code AbstractComboBoxBrowser} to complete relative and
 * absolute path names of files and directories.
 * This class uses a {@link FileSystemView} in order to create file objects
 * for auto completion.
 * <p>
 * To use it, use something like this:
 * <pre>
    JComboBox box = new JComboBox();
    new FileComboBoxBrowser(box);
    box.setEditable(true);
 * </pre>
 *
 * @since  TrueCommons 2.2
 * @author Christian Schlichtherle
 */
public class FileComboBoxBrowser extends AbstractComboBoxBrowser<String> {

    private static final long serialVersionUID = -6878885832542209810L;

    private transient @Nullable FileSystemView fsv;
    private transient @Nullable File dir;

    /**
     * Constructs a new file combo box auto completion browser.
     * {@link #setComboBox} must be called in order to use this object.
     */
    public FileComboBoxBrowser() { this(null); }

    /**
     * Creates a new combo box auto completion browser.
     *
     * @param comboBox The combo box to enable browsing for auto completions.
     *        May be {@code null}.
     */
    public FileComboBoxBrowser(final @Nullable JComboBox<String> comboBox) {
        super(comboBox);
        if (null != comboBox)
            update0(Objects.toString(comboBox.getSelectedItem(), ""));
    }

    /**
     * Returns the file system view.
     * If this property has never been initialized or has been explicitly set
     * to {@code null}, then a call to this method reinitializes it by calling
     * {@link FileSystemView#getFileSystemView}.
     */
    public FileSystemView getFileSystemView() {
        final FileSystemView fsv = this.fsv;
        return null != fsv
                ? fsv
                : (this.fsv = FileSystemView.getFileSystemView());

    }

    /** Sets the file system view. */
    public void setFileSystemView(final @Nullable FileSystemView fsv) {
        this.fsv = fsv;
    }

    /**
     * Returns the directory which is used for autocompleting relative path
     * names.
     * If this property has never been initialized or has been explicitly set
     * to {@code null}, then a call to this method reinitializes it by calling
     * {@link FileSystemView#getDefaultDirectory} on the
     * {@linkplain #getFileSystemView file system view}.
     */
    public File getDirectory() {
        final File dir = this.dir;
        return null != dir
                ? dir
                : (this.dir = getFileSystemView().getDefaultDirectory());
    }

    /**
     * Sets the directory which is used for autocompleting relative path names.
     */
    public void setDirectory(final @Nullable File dir) {
        this.dir = dir;
    }

    /**
     * Interpretes the specified {@code initials} as the initial
     * characters of an absolute or relative path name of a node in the file
     * system and updates the contents of the combo box model with possible
     * completions.
     * The elements in the combo box model are sorted according to their
     * natural comparison order.
     *
     * @param  initials The initial characters of a file or directory path name.
     * @return {@code true} if and only if the file system contains a
     *         node with {@code initials} as its initial characters and
     *         hence the popup window with the completions should be shown.
     * @throws NullPointerException If the {@code comboBox} property is
     *         {@code null}.
     */
    @Override
    protected boolean update(String initials) {
        return update0(initials);
    }

    private boolean update0(final String initials) {
        File dir = getDirectory();

        // This is actually a pretty ugly piece of code, but I don't know
        // of any other way to implement this so that a user can use his
        // platform specific file separator (e.g. '\\' on Windows)
        // AND the standard URI name separator '/' in a file path
        // name.

        // Identify the directory to list, the prefix of the elements we want
        // to find and the base string which prepends the elements we will find.
        final String prefix, base;
        if ("".equals(initials)) {
            prefix = base = "";
        } else {
            File node = getFileSystemView().createFileObject(initials);
            if (node.isAbsolute()) {
                final boolean dirPath = node.getPath().length() < initials.length();
                // The test order is important here because isDirectory() may
                // actually prompt the user for a key if node is an RAES
                // encrypted ZIP file!
                if (dirPath && node.isDirectory()) {
                    dir = node;
                    prefix = "";
                } else {
                    dir = node.getParentFile();
                    if (dir == null) {
                        dir = node;
                        prefix = "";
                    } else {
                        prefix = node.getName();
                    }
                }
                if (dir.getPath().endsWith(File.separator)) {
                    // dir is the root of a file system.
                    base = initials.substring(0, dir.getPath().length());
                } else {
                    // Otherwise keep the user provided file separator.
                    base = initials.substring(0, dir.getPath().length() + 1);
                }
            } else {
                final File directory = dir;
                node = getFileSystemView().createFileObject(directory, initials); // copies archive detector from directory
                final boolean dirPath = node.getPath().length()
                        < (directory.getPath() + File.separator + initials).length();
                // The test order is important here because isDirectory() may
                // actually prompt the user!
                if (dirPath && node.isDirectory()) {
                    dir = node;
                    prefix = "";
                } else {
                    dir = node.getParentFile();
                    assert null != dir;
                    prefix = node.getName();
                }
                // Keep the platform specific file separator.
                base = initials.substring(0, dir.getPath().length() - directory.getPath().length());
            }
        }

        class Filter implements FilenameFilter {
            final int pl = prefix.length();

            @Override
            public boolean accept(File d, String child) {
                // Always ignore case, not just on Windoze.
                return pl <= child.length()
                        ? prefix.equalsIgnoreCase(child.substring(0, pl))
                        : false;
            }
        } // Filter
        final String[] children = dir.list(new Filter());

        // Update combo box model.
        // Note that the list MUST be cleared and repopulated because its
        // current content does not need to reflect the status of the edited
        // initials.
        final DefaultComboBoxModel<String> model = (DefaultComboBoxModel<String>) getComboBox().getModel();
        try {
            model.removeAllElements();
            final int l = null == children ? 0 : children.length;
            if (0 < l) {
                Arrays.sort(children, Collator.getInstance()); // get nice sorting order
                for (int i = 0; i < l; i++)
                    model.addElement(base + children[i]);
                return true; // show popup
            } else {
                // Leave initials als sole content of the list.
                model.addElement(initials);
                return false; // hide popup
            }
        } finally {
            if (!initials.equals(model.getSelectedItem())) // check required!
                model.setSelectedItem(initials);
        }
    }
}
