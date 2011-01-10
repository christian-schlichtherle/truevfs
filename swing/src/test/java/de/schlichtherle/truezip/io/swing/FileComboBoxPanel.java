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

import de.schlichtherle.truezip.file.File;
import java.awt.EventQueue;
import javax.swing.JFrame;
import javax.swing.WindowConstants;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class FileComboBoxPanel extends javax.swing.JPanel implements Runnable {
    private static final long serialVersionUID = 1065812374938719922L;

    /** Creates new form FileComboBoxPanel */
    public FileComboBoxPanel() {
        initComponents();
    }

    /** Creates new form FileComboBoxPanel */
    public FileComboBoxPanel(java.io.File directory) {
        initComponents();
        setDirectoryImpl(directory);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        final javax.swing.JLabel jLabel1 = new javax.swing.JLabel();
        final javax.swing.JComboBox box1 = new javax.swing.JComboBox();
        final javax.swing.JComboBox box2 = new javax.swing.JComboBox();

        browser1.setComboBox(box1);
        browser2.setComboBox(box2);

        setLayout(new java.awt.GridBagLayout());

        setBorder(javax.swing.BorderFactory.createEmptyBorder(15, 15, 15, 15));
        jLabel1.setFont(new java.awt.Font("Dialog", 1, 12));
        jLabel1.setText("Please start entering a file name anywhere...");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(50, 0, 0, 0);
        add(jLabel1, gridBagConstraints);

        box1.setEditable(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        add(box1, gridBagConstraints);

        box2.setEditable(true);
        box2.setModel(box1.getModel());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(50, 0, 0, 0);
        add(box2, gridBagConstraints);

    }// </editor-fold>//GEN-END:initComponents
    
    /**
     * Getter for property directory.
     * @return Value of property directory.
     */
    public java.io.File getDirectory() {
        return browser1.getDirectory();
    }

    /**
     * Setter for property directory.
     * @param directory New value of property directory.
     */
    public void setDirectory(java.io.File directory) {
        setDirectoryImpl(directory);
    }

    private void setDirectoryImpl(java.io.File directory) {
        browser1.setDirectory(directory);
        browser2.setDirectory(directory);
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(new FileComboBoxPanel(new File(args[0])));
    }

    @Override
	public void run() {
        EventQueue.invokeLater(new Runnable() {
            @Override
			public void run() {
                final JFrame frame = new JFrame("File name auto completion fun");
                frame.getContentPane().add(FileComboBoxPanel.this);
                frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private final de.schlichtherle.truezip.io.swing.FileComboBoxBrowser browser1 = new de.schlichtherle.truezip.io.swing.FileComboBoxBrowser();
    private final de.schlichtherle.truezip.io.swing.FileComboBoxBrowser browser2 = new de.schlichtherle.truezip.io.swing.FileComboBoxBrowser();
    // End of variables declaration//GEN-END:variables
}
