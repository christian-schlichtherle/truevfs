/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.key.swing;

import net.java.truecommons.key.swing.io.FileComboBoxBrowser;

import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.io.File;
import java.util.ResourceBundle;

/**
 * A panel displaying a password panel or a key file panel in order to let
 * the user select an authentication method and enter a key.
 *
 * @since  TrueCommons 2.2
 * @author Christian Schlichtherle
 */
public final class AuthenticationPanel extends JPanel {

    private static final long serialVersionUID = 0L;

    private static final ResourceBundle resources = ResourceBundle
            .getBundle(AuthenticationPanel.class.getName());

    /** The password authentication method. */
    static final int AUTH_PASSWD = 0;

    /** The key file authentication method. */
    static final int AUTH_KEY_FILE = 1;

    private final FileComboBoxBrowser fcbb;

    public AuthenticationPanel() {
        // Order is important here: The file combo box browser installs its
        // own editor, so we have to adjust the columns last.
        fcbb = new FileComboBoxBrowser(keyFile);
        initComponents();
        ((JTextField) keyFile.getEditor().getEditorComponent()).setColumns(30);
    }

    /**
     * Returns the file system view which is used for the key file combo box
     * and its associated file chooser.
     * If this property has never been initialized or has been explicitly set
     * to {@code null}, then a call to this method reinitializes it by calling
     * {@link FileSystemView#getFileSystemView}.
     */
    public FileSystemView getFileSystemView() {
        return fcbb.getFileSystemView();
    }

    /**
     * Sets the file system view which is used for the key file combo box
     * and its associated file chooser.
     */
    public void setFileSystemView(@Nullable FileSystemView fsv) {
        fcbb.setFileSystemView(fsv);
    }

    /**
     * Returns the directory which is used for the key file combo box
     * and its associated file chooser.
     * If this property has never been initialized or has been explicitly set
     * to {@code null}, then a call to this method reinitializes it by calling
     * {@link FileSystemView#getDefaultDirectory} on the
     * {@linkplain #getFileSystemView file system view}.
     */
    public File getDirectory() { return fcbb.getDirectory(); }

    /**
     * Sets the directory which is used for the key file combo box and its
     * associated file chooser.
     */
    public void setDirectory(@Nullable File dir) {
        fcbb.setDirectory(dir);
    }

    /**
     * Sets the panel which should be used to enter the password.
     *
     * @param  passwdPanel the password panel.
     * @throws NullPointerException If {@code passwdPanel} is
     *         {@code null}.
     */
    public void setPasswdPanel(final JPanel passwdPanel) {
        passwdPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        final String title = resources.getString("tab.passwd");
        if (title.equals(tabs.getTitleAt(AUTH_PASSWD)))
            tabs.removeTabAt(AUTH_PASSWD);
        tabs.insertTab(title, null, passwdPanel, null, AUTH_PASSWD); // NOI18N
        tabs.setSelectedIndex(AUTH_PASSWD);
        revalidate();
    }

    Document getKeyFileDocument() {
        return ((JTextComponent) keyFile.getEditor().getEditorComponent()).getDocument();
    }

    /**
     * Returns the key file.
     *
     * @return The key file.
     */
    File getKeyFile() {
        String path = (String) keyFile.getSelectedItem();
        File file = new File(path);
        return file.isAbsolute() ? file : new File(getDirectory(), path);
    }

    private void setKeyFile(final File file) {
        String newPath = file.getPath();
        {
            final String dir = getDirectory().getPath();
            if (newPath.startsWith(dir))
                newPath = newPath.substring(dir.length() + 1); // cut off file separator, too.
        }
        final String oldPath = (String) keyFile.getSelectedItem();
        if (newPath.equals(oldPath)) return;
        keyFile.setSelectedItem(newPath);
    }

    /**
     * Returns the authentication method selected by the user.
     *
     * @return {@code AUTH_PASSWD} or {@code AUTH_KEY_FILE}.
     */
    int getAuthenticationMethod() {
        final int method = tabs.getSelectedIndex();
        switch (method) {
            case AUTH_PASSWD:
                assert resources.getString("tab.passwd").equals(tabs.getTitleAt(method));
                break;
            case AUTH_KEY_FILE:
                assert resources.getString("tab.keyFile").equals(tabs.getTitleAt(method));
                break;
            default:
                throw new AssertionError("Unsupported authentication method!");
        }
        return method;
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        final net.java.truecommons.key.swing.util.EnhancedPanel keyFilePanel = new net.java.truecommons.key.swing.util.EnhancedPanel();
        final javax.swing.JLabel keyFileLabel = new javax.swing.JLabel();
        final javax.swing.JButton keyFileChooser = new javax.swing.JButton();

        setLayout(new java.awt.GridBagLayout());

        keyFilePanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        keyFilePanel.addPanelListener(new net.java.truecommons.key.swing.util.PanelListener() {
            public void ancestorWindowShown(net.java.truecommons.key.swing.util.PanelEvent evt) {
                keyFilePanelAncestorWindowShown(evt);
            }
            public void ancestorWindowHidden(net.java.truecommons.key.swing.util.PanelEvent evt) {
            }
        });
        keyFilePanel.setLayout(new java.awt.GridBagLayout());

        keyFileLabel.setDisplayedMnemonic(resources.getString("keyFile").charAt(0));
        keyFileLabel.setLabelFor(keyFile);
        keyFileLabel.setText(resources.getString("keyFile")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
        keyFilePanel.add(keyFileLabel, gridBagConstraints);

        keyFile.setEditable(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        keyFilePanel.add(keyFile, gridBagConstraints);

        keyFileChooser.setIcon(UIManager.getIcon("FileView.directoryIcon"));
        keyFileChooser.setToolTipText(resources.getString("selectKeyFile.toolTip")); // NOI18N
        keyFileChooser.setName("keyFileChooser"); // NOI18N
        keyFileChooser.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                keyFileChooserActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        keyFilePanel.add(keyFileChooser, gridBagConstraints);

        tabs.addTab(resources.getString("tab.keyFile"), keyFilePanel); // NOI18N

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        add(tabs, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents

    private void keyFileChooserActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_keyFileChooserActionPerformed
        final JFileChooser fc = newFileChooser();
        if (JFileChooser.APPROVE_OPTION == fc.showOpenDialog(this))
            setKeyFile(fc.getSelectedFile());
    }//GEN-LAST:event_keyFileChooserActionPerformed

    private JFileChooser newFileChooser() {
        final JFileChooser fc = new JFileChooser(getDirectory());
        fc.setDialogTitle(resources.getString("fileChooser.title"));
        fc.setFileHidingEnabled(false);
        return fc;
    }

    private void keyFilePanelAncestorWindowShown(net.java.truecommons.key.swing.util.PanelEvent evt) {//GEN-FIRST:event_keyFilePanelAncestorWindowShown
        // These are the things I hate Swing for: All I want to do here is to
        // set the focus to the keyFile field in this panel when it shows.
        // However, this can't be done in the constructor since the panel is
        // not yet placed in a window which is actually showing.
        // Right, then I use this event listener to do it. This listener
        // method is called when the ancestor window is showing (and coding
        // the event generation was a less than trivial task).
        // But wait, simply setting the focus in this event listener here is
        // not possible on Linux because the containing window (now there is
        // one) didn't gain the focus yet.
        // Strangely enough, this works on Windows.
        // Even more strange, not even calling passwd.requestFocus() makes it
        // work on Linux!
        // So I add a window focus listener here and remove it when a Focus
        // Gained Event occurs.
        // But wait, then I still can't request the focus: This time it
        // doesn't work on Windows, while it works on Linux.
        // I still don't know the reason why, but it seems it's moving too
        // fast, so I have to post a new event to the event queue which finally
        // sets the focus.
        // But wait, requesting the focus could still fail for some strange,
        // undocumented reason - I wouldn't be surprised anymore.
        // So I add a conditional to select the entire contents of the field
        // only if I can really transfer the focus to it.
        // Otherwise, users could get easily confused.
        // If you carefully read the documentation for requestFocusInWindow()
        // however, then you learn that even if it returns true, there is still
        // no guarantee that the focus gets actually transferred...
        // This mess is insane!
        final Window window = evt.getSource().getAncestorWindow();
        window.addWindowFocusListener(new WindowFocusListener() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                window.removeWindowFocusListener(this);
                EventQueue.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        if (keyFile.requestFocusInWindow())
                            ((JTextComponent) keyFile.getEditor().getEditorComponent()).selectAll();
                    }
                });
            }

            @Override
            public void windowLostFocus(WindowEvent e) {
            }
        });
    }//GEN-LAST:event_keyFilePanelAncestorWindowShown

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private final javax.swing.JComboBox<String> keyFile = new javax.swing.JComboBox<String>();
    private final javax.swing.JTabbedPane tabs = new javax.swing.JTabbedPane();
    // End of variables declaration//GEN-END:variables
}
