/*
 * Copyright (C) 2005-2010 Schlichtherle IT Services
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

import de.schlichtherle.io.*;

import java.awt.*;
import java.io.*;

import javax.swing.*;

/**
 * A custom <code>JFileChooser</code> which supports browsing archive files
 * like (virtual) directories.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class JFileChooser extends javax.swing.JFileChooser {

    private transient volatile short paintingDisabled; // = false;

    public JFileChooser() {
        super(FileSystemView.getFileSystemView());
        super.setFileView(new FileView(super.getFileView()));
        super.setDoubleBuffered(false);
    }

    public JFileChooser(ArchiveDetector archiveDetector) {
        super(FileSystemView.getFileSystemView(archiveDetector));
        super.setFileView(new FileView(super.getFileView()));
        super.setDoubleBuffered(false);
    }

    /**
     * Returns a {@link de.schlichtherle.io.File de.schlichtherle.io.File}
     * instead of {@link java.io.File java.io.File}.
     *
     * @see javax.swing.JFileChooser#getSelectedFile()
     */
    public java.io.File getSelectedFile() {
        java.io.File file = super.getSelectedFile();
        return ((FileSystemView) getFileSystemView()).wrap(file);
    }

    /**
     * Returns an array of
     * {@link de.schlichtherle.io.File de.schlichtherle.io.File}
     * objects instead of {@link java.io.File java.io.File} objects.
     *
     * @see javax.swing.JFileChooser#getSelectedFiles()
     */
    public java.io.File[] getSelectedFiles() {
        java.io.File files[] = super.getSelectedFiles();
        if (files != null) {
            FileSystemView fsv = (FileSystemView) getFileSystemView();
            for (int i = files.length; --i >= 0; ) {
                files[i] = fsv.wrap(files[i]);
                //files[i] = files[i] != null ? new File(files[i]) : null;
            }
        }
        return files;
    }

    public Icon getIcon(final java.io.File file) {
        beginPaintingDisabled();
        try {
            return super.getIcon(file);
        } finally {
            endPaintingDisabled();
        }
    }

    public String getTypeDescription(final java.io.File file) {
        beginPaintingDisabled();
        try {
            return super.getTypeDescription(file);
        } finally {
            endPaintingDisabled();
        }
    }

    public boolean isTraversable(final java.io.File file) {
        beginPaintingDisabled();
        try {
            return super.isTraversable(file);
        } finally {
            endPaintingDisabled();
        }
    }

    public void paint(final Graphics g) {
        if (paintingDisabled > 0) {
            /*EventQueue.invokeLater(new Runnable() {
                public void run() {
                    JFileChooser.super.paint(g);
                }
            });*/
        } else {
            beginPaintingDisabled();
            try {
                super.paintChildren(g);
            } finally {
                endPaintingDisabled();
            }
        }
    }

    private synchronized void beginPaintingDisabled() {
        paintingDisabled++;
    }

    private synchronized void endPaintingDisabled() {
        paintingDisabled--;
        if (paintingDisabled <= 0)
            repaint();
    }
}
