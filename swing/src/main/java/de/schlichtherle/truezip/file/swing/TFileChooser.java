/*
 * Copyright (C) 2005-2011 Schlichtherle IT Services
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

import de.schlichtherle.truezip.file.TArchiveDetector;
import de.schlichtherle.truezip.file.TFile;
import java.awt.Graphics;
import java.io.File;
import javax.swing.Icon;
import javax.swing.JFileChooser;

/**
 * A custom {@code TFileChooser} which supports browsing archive files
 * like (virtual) directories.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class TFileChooser extends JFileChooser {
    private static final long serialVersionUID = 936528972682036204L;

    private transient volatile short paintingDisabled; // = false;

    public TFileChooser() {
        super(TFileSystemView.getFileSystemView());
        super.setFileView(new TFileView(super.getFileView()));
        super.setDoubleBuffered(false);
    }

    public TFileChooser(TArchiveDetector archiveDetector) {
        super(TFileSystemView.getFileSystemView(archiveDetector));
        super.setFileView(new TFileView(super.getFileView()));
        super.setDoubleBuffered(false);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Note that this method returns a {@link TFile} instead of a {@link File}.
     */
    @Override
    public File getSelectedFile() {
        File file = super.getSelectedFile();
        return ((TFileSystemView) getFileSystemView()).wrap(file);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Note that this method returns an array of {@link TFile} objects instead
     * of {@link File} objects.
     */
    @Override
    public File[] getSelectedFiles() {
        File files[] = super.getSelectedFiles();
        if (files != null) {
            TFileSystemView fsv = (TFileSystemView) getFileSystemView();
            for (int i = files.length; --i >= 0; ) {
                files[i] = fsv.wrap(files[i]);
                //files[i] = files[i] != null ? new File(files[i]) : null;
            }
        }
        return files;
    }

    @Override
    public Icon getIcon(final File file) {
        beginPaintingDisabled();
        try {
            return super.getIcon(file);
        } finally {
            endPaintingDisabled();
        }
    }

    @Override
    public String getTypeDescription(final File file) {
        beginPaintingDisabled();
        try {
            return super.getTypeDescription(file);
        } finally {
            endPaintingDisabled();
        }
    }

    @Override
    public boolean isTraversable(final File file) {
        beginPaintingDisabled();
        try {
            return super.isTraversable(file);
        } finally {
            endPaintingDisabled();
        }
    }

    @Override
    public void paint(final Graphics g) {
        if (paintingDisabled > 0) {
            /*EventQueue.invokeLater(new Runnable() {
                public void run() {
                    TFileChooser.super.paint(g);
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
