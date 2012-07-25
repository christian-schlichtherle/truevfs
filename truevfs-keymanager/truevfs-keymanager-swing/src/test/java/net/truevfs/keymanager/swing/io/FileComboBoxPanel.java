/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.keymanager.swing.io;

import java.io.File;
import javax.annotation.CheckForNull;
import javax.swing.JPanel;

/**
 * @author Christian Schlichtherle
 */
public class FileComboBoxPanel extends JPanel {
    private static final long serialVersionUID = 1065812374938719922L;

    /** Creates new form FileComboBoxPanel */
    public FileComboBoxPanel() {
        initComponents();
    }

    /** Creates new form FileComboBoxPanel */
    public FileComboBoxPanel(@CheckForNull File directory) {
        initComponents();
        setDirectory0(directory);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        final javax.swing.JLabel jLabel1 = new javax.swing.JLabel();
        final javax.swing.JComboBox box1 = new javax.swing.JComboBox();
        final javax.swing.JComboBox box2 = new javax.swing.JComboBox();

        browser1.setComboBox(box1);

        browser2.setComboBox(box2);

        setBorder(javax.swing.BorderFactory.createEmptyBorder(15, 15, 15, 15));
        setLayout(new java.awt.GridBagLayout());

        jLabel1.setFont(new java.awt.Font("Dialog", 1, 12)); // NOI18N
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
    public File getDirectory() {
        return browser1.getDirectory();
    }

    /**
     * Setter for property directory.
     * @param directory New value of property directory.
     */
    public void setDirectory(@CheckForNull File directory) {
        setDirectory0(directory);
    }

    private void setDirectory0(@CheckForNull File directory) {
        browser1.setDirectory(directory);
        browser2.setDirectory(directory);
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private final net.truevfs.keymanager.swing.io.FileComboBoxBrowser browser1 = new net.truevfs.keymanager.swing.io.FileComboBoxBrowser();
    private final net.truevfs.keymanager.swing.io.FileComboBoxBrowser browser2 = new net.truevfs.keymanager.swing.io.FileComboBoxBrowser();
    // End of variables declaration//GEN-END:variables
}