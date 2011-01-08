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

package de.schlichtherle.truezip.io.swing;

import de.schlichtherle.truezip.io.file.ArchiveDetector;
import de.schlichtherle.truezip.io.file.File;
import de.schlichtherle.truezip.swing.AbstractComboBoxBrowser;
import java.io.FilenameFilter;
import java.text.Collator;
import java.util.Arrays;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;

/**
 * Subclasses {@code AbstractComboBoxBrowser} to complete relative and
 * absolute path names of files and directories.
 * This class uses instances of TrueZIP's custom {@link File} class in
 * order to support the browsing of archive files for auto completion.
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
public class FileComboBoxBrowser extends AbstractComboBoxBrowser {

    private static final long serialVersionUID = -6878885832542209810L;

    private java.io.File directory = new File(".");

    /**
     * Creates a new combo box auto completion browser.
     * {@link #setComboBox} must be called in order to use this object.
     */
    public FileComboBoxBrowser() {
    }

    /**
     * Creates a new combo box auto completion browser.
     *
     * @param comboBox The combo box to enable browsing for auto completions.
     *        May be {@code null}.
     */
    public FileComboBoxBrowser(final JComboBox comboBox) {
        super(comboBox);
        if (comboBox != null) {
            final Object item = comboBox.getSelectedItem();
            if (item == null || item instanceof String)
                update( (DefaultComboBoxModel) comboBox.getModel(),
                        directory, (String) item);
        }
    }

    /**
     * Returns the directory which is used to complete relative path names.
     * This defaults to the current directory; {@code null} is never
     * returned.
     */
    public java.io.File getDirectory() {
        return directory;
    }

    /**
     * Returns the directory which is used to complete relative path names.
     *
     * @param directory The directory to use for completion.
     *        If this is {@code null}, the directory is reset to the
     *        current directory.
     */
    public void setDirectory(final java.io.File directory) {
        this.directory = directory != null ? directory : new File(".");
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
    protected boolean update(final String initials) {
        final JComboBox cb = getComboBox();
        return update(  (DefaultComboBoxModel) cb.getModel(),
                        getDirectory(), initials);
    }

    private static boolean update(
            final DefaultComboBoxModel model,
            java.io.File dir,
            String initials) {
        // This is actually a pretty ugly piece of code, but I don't know
        // of any other way to implement this so that a user can use his
        // platform specific file separator (e.g. '\\' on Windows)
        // AND the standard Internet file name separator '/' in a file path
        // name.

        if (initials == null)
            initials = "";

        // Identify the directory to list, the prefix of the elements we want
        // to find and the base string which prepends the elements we will find.
        final String prefix, base;
        if ("".equals(initials)) {
            prefix = base = "";
        } else {
            final ArchiveDetector detector;
            if (dir instanceof File)
                detector = ((File) dir).getArchiveDetector();
            else
                detector = ArchiveDetector.NULL;
            File node = new File(initials, detector);
            if (node.isAbsolute()) {
                final boolean dirPath = node.getPath().length() < initials.length();
                // FIXME: Integrate this into truezip-driver-tzp!
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
                final java.io.File directory = dir;
                node = new File(directory, initials); // inherits archive detector from directory
                final boolean dirPath = node.getPath().length()
                        < (directory.getPath() + File.separator + initials).length();
                // FIXME: Integrate this into truezip-driver-tzp!
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
                // Keep the user provided file separator.
                base = initials.substring(0, dir.getPath().length() - directory.getPath().length());
            }
        }

        final FilenameFilter filter;
        /*if (File.separatorChar != '\\') {
            // Consider case (Unix).
            filter = new FilenameFilter() {
                public boolean accept(java.io.File d, String child) {
                    return child.startsWith(prefix);
                }
            };
        } else {*/
            // Ignore case (Windows).
            filter = new FilenameFilter() {
                final int pl = prefix.length();

                @Override
				public boolean accept(java.io.File d, String child) {
                    if (child.length() >= pl)
                        return prefix.equalsIgnoreCase(child.substring(0, pl));
                    else
                        return false;
                }
            };
        //}
        final String[] children = dir.list(filter);

        // Update combo box model.
        // Note that the list MUST be cleared and repopulated because its
        // current content does not need to reflect the status of the edited
        // initials.
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
