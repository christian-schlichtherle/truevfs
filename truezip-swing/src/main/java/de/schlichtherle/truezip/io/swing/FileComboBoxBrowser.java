/*
 * Copyright (C) 2006-2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.io.swing;

import de.schlichtherle.truezip.swing.AbstractComboBoxBrowser;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.File;
import java.io.FilenameFilter;
import java.text.Collator;
import java.util.Arrays;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.filechooser.FileSystemView;

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
 * @author Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
public class FileComboBoxBrowser extends AbstractComboBoxBrowser<String> {

    private static final long serialVersionUID = -6878885832542209810L;

    private transient @CheckForNull FileSystemView fileSystemView;
    private transient @CheckForNull File directory;

    /**
     * Constructs a new file combo box auto completion browser.
     * {@link #setComboBox} must be called in order to use this object.
     */
    public FileComboBoxBrowser() {
        this(null, null);
    }

    public FileComboBoxBrowser(@CheckForNull JComboBox<String> comboBox) {
        this(comboBox, null);
    }

    public FileComboBoxBrowser(@CheckForNull FileSystemView fileSystemView) {
        this(null, fileSystemView);
    }

    /**
     * Creates a new combo box auto completion browser.
     *
     * @param comboBox The combo box to enable browsing for auto completions.
     *        May be {@code null}.
     */
    public FileComboBoxBrowser( final @CheckForNull JComboBox<String> comboBox,
                                @CheckForNull FileSystemView fileSystemView) {
        super(comboBox);
        this.fileSystemView = fileSystemView;
        if (null != comboBox) {
            Object item = comboBox.getSelectedItem();
            if (null == item || item instanceof String)
                update0((String) item);
        }
    }

    /**
     * Returns the directory which is used for autocompleting relative path
     * names.
     * If this property has been set to {@code null} before, it's reinitialized
     * by calling
     * {@link FileSystemView#createFileObject(String) createFileObject(".")} on
     * the current file system view, so {@code null} is never returned.
     *
     * @return The directory which is used for autocompleting relative path
     *         names.
     */
    public File getDirectory() {
        final File directory = this.directory;
        return null != directory
                ? directory
                : (this.directory = getFileSystemView().createFileObject("."));
    }

    /**
     * Sets the directory which is used for autocompleting relative path names.
     *
     * @param directory The directory to use for autocompletion.
     *        If this is {@code null}, the directory is reset to the
     *        current directory.
     */
    public void setDirectory(final @CheckForNull File directory) {
        this.directory = directory;
    }

    /**
     * Returns the file system view.
     * If this property has been set to {@code null} before, it's reinitialized
     * by calling {@link FileSystemView#getFileSystemView()}, so {@code null}
     * is never returned.
     * 
     * @return The file system view.
     */
    public FileSystemView getFileSystemView() {
        final FileSystemView fileSystemView = this.fileSystemView;
        return null != fileSystemView
                ? fileSystemView
                : (this.fileSystemView = FileSystemView.getFileSystemView());

    }

    /**
     * Sets the file system view.
     *
     * @param fileSystemView the file system view.
     */
    public void setFileSystemView(final @CheckForNull FileSystemView fileSystemView) {
        this.fileSystemView = fileSystemView;
    }

    /**
     * Interpretes the specified {@code initials} as the initial
     * characters of an absolute or relative path name of a node in the file
     * system and updates the contents of the combo box model with possible
     * completions.
     * The elements in the combo box model are sorted according to their
     * natural comparison order.
     *
     * @param initials The initial characters of a file or directory path name.
     *        May be {@code null}.
     * @return {@code true} if and only if the file system contains a
     *         node with {@code initials} as its initial characters and
     *         hence the popup window with the completions should be shown.
     * @throws NullPointerException If the {@code comboBox} property is
     *         {@code null}.
     */
    @Override
    protected boolean update(@CheckForNull String initials) {
        return update0(initials);
    }

    private boolean update0(@CheckForNull String initials) {
        File dir = getDirectory();

        // This is actually a pretty ugly piece of code, but I don't know
        // of any other way to implement this so that a user can use his
        // platform specific file separator (e.g. '\\' on Windows)
        // AND the standard URI name separator '/' in a file path
        // name.

        if (null == initials)
            initials = "";

        // Identify the directory to list, the prefix of the elements we want
        // to find and the base string which prepends the elements we will find.
        final String prefix, base;
        if ("".equals(initials)) {
            prefix = base = "";
        } else {
            File node = getFileSystemView().createFileObject(initials);
            if (node.isAbsolute()) {
                final boolean dirPath = node.getPath().length() < initials.length();
                // TODO: Evaluate why this was needed in TrueZIP 6 and if it's
                // still required in TrueZIP 7.
                /*if (dirPath)
                    PromptingKeyManager.resetCancelledPrompts();*/
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
                // TODO: Evaluate why this was needed in TrueZIP 6 and if it's
                // still required in TrueZIP 7.
                /*if (dirPath)
                    PromptingKeyManager.resetCancelledPrompts();*/
                // The test order is important here because isDirectory() may
                // actually prompt the user!
                if (dirPath && node.isDirectory()) {
                    dir = node;
                    prefix = "";
                } else {
                    dir = node.getParentFile();
                    assert dir != null : "node is child of directory";
                    prefix = node.getName();
                }
                // Keep the platform specific file separator.
                base = initials.substring(0, dir.getPath().length() - directory.getPath().length());
            }
        }

        final FilenameFilter filter;
        /*if (TFile.separatorChar != '\\') {
            // Consider case (Unix).
            filter = new FilenameFilter() {
                public boolean accept(java.io.TFile d, String child) {
                    return child.startsWith(prefix);
                }
            };
        } else {*/
            // Ignore case (Windows).
            class Filter implements FilenameFilter {
                final int pl = prefix.length();

                @Override
                public boolean accept(File d, String child) {
                    if (child.length() >= pl)
                        return prefix.equalsIgnoreCase(child.substring(0, pl));
                    else
                        return false;
                }
            } // class Filter
            filter = new Filter();
        //}
        final String[] children = dir.list(filter);

        // Update combo box model.
        // Note that the list MUST be cleared and repopulated because its
        // current content does not need to reflect the status of the edited
        // initials.
        final DefaultComboBoxModel<String> model = (DefaultComboBoxModel<String>) getComboBox().getModel();
        try {
            model.removeAllElements();
            final int l = children != null ? children.length : 0;
            if (l > 0) {
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
