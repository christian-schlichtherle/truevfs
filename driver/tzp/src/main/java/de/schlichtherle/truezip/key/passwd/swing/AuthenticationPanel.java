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

package de.schlichtherle.truezip.key.passwd.swing;

import de.schlichtherle.truezip.io.file.ArchiveDetector;
import de.schlichtherle.truezip.io.file.File;
import de.schlichtherle.truezip.io.swing.FileComboBoxBrowser;
import java.awt.EventQueue;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.lang.ref.SoftReference;
import java.util.ResourceBundle;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

/**
 * A panel displaying a password panel or a key file panel in order to let
 * the user select an authentication method and enter the key.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class AuthenticationPanel extends JPanel {

    private static final String CLASS_NAME = AuthenticationPanel.class.getName();
    private static final ResourceBundle resources
            = ResourceBundle.getBundle(CLASS_NAME);
    private static final File BASE_DIR = new File(".", ArchiveDetector.NULL);

    private static SoftReference<javax.swing.JFileChooser> fileChooser;

    /** The password authentication method. */
    public static final int AUTH_PASSWD = 0;
    
    /** The key file authentication method. */
    public static final int AUTH_KEY_FILE = 1;
    private static final long serialVersionUID = 3876515923659236921L;

    /**
     * Creates a new authentication panel.
     * This version of the constructor does not remember the key file path.
     */
    public AuthenticationPanel() {
        initComponents();

        // Order is important here: The file combo box browser installs its
        // own editor, so we have to adjust the columns last.
        new FileComboBoxBrowser(keyFile).setDirectory(BASE_DIR);
        ((JTextField) keyFile.getEditor().getEditorComponent()).setColumns(30);
    }

    /**
     * Sets the panel which should be used to enter the password.
     *
     * @throws NullPointerException If {@code passwdPanel} is
     *         {@code null}.
     */
    public void setPasswdPanel(JPanel passwdPanel) {
        if (passwdPanel == null)
            throw new NullPointerException();

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
     * Returns the path of the key file.
     * If the parameter {@code rememberPath} of the constructor was
     * {@code true}, then the returned path is remembered in a static
     * field for the next instance of this class.
     */
    public String getKeyFilePath() {
        return (String) keyFile.getSelectedItem();
    }

    private void setKeyFilePath(final String path) {
        final String oldPath = (String) keyFile.getSelectedItem();
        if (null != path && path.equals(oldPath))
            return;

        keyFile.setSelectedItem(path);
        /*final Window window = SwingUtilities.getWindowAncestor(this);
        if (window != null)
            window.pack();*/
    }

    /**
     * Returns the authentication method selected by the user.
     *
     * @return {@code AUTH_PASSWD} or {@code AUTH_KEY_FILE}.
     */
    public int getAuthenticationMethod() {
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

    /**
     * Return a {@code JFileChooser} to use within this panel.
     * The file chooser is stored in a cache for subsequent use.
     * If the JVM gets short of storage, the cache is emptied and a new
     * file chooser is instantiated on the next call to this method again.
     * In any way, the file chooser will always remember its current directory.
     * In addition, the returned file chooser has file hiding disabled.
     * Note that the file chooser is a plain javax.swing.FileChooser which
     * does <em>not</em> support archive browsing to prevent illegal recursion.
     */
    static javax.swing.JFileChooser getFileChooser() {
        final SoftReference<javax.swing.JFileChooser> ref = fileChooser; // cache
        javax.swing.JFileChooser fc = ref != null ? ref.get() : null;
        if (fc == null) {
            fc = new CustomFileChooser();
            fileChooser = new SoftReference<javax.swing.JFileChooser>(fc);
        }
        return fc;
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        tabs = new javax.swing.JTabbedPane();
        final javax.swing.JLabel keyFileLabel = new javax.swing.JLabel();

        setLayout(new java.awt.GridBagLayout());

        keyFilePanel.setLayout(new java.awt.GridBagLayout());

        keyFilePanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        keyFilePanel.addPanelListener(new de.schlichtherle.truezip.swing.event.PanelListener() {
            @Override
			public void ancestorWindowShown(de.schlichtherle.truezip.swing.event.PanelEvent evt) {
                keyFilePanelAncestorWindowShown(evt);
            }
            @Override
			public void ancestorWindowHidden(de.schlichtherle.truezip.swing.event.PanelEvent evt) {
            }
        });

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
        keyFileChooser.setName("keyFileChooser");
        keyFileChooser.addActionListener(new java.awt.event.ActionListener() {
            @Override
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
        final javax.swing.JFileChooser fc = getFileChooser();
        if (fc.showOpenDialog(this) == javax.swing.JFileChooser.APPROVE_OPTION) {
            final File file = new File(fc.getSelectedFile(), ArchiveDetector.NULL);
            final String baseDirPath = BASE_DIR.getCanOrAbsPath();
            String keyFilePath = file.getCanOrAbsPath();
            if (keyFilePath.startsWith(baseDirPath)) {
                assert keyFilePath.charAt(baseDirPath.length()) == File.separatorChar;
                keyFilePath = keyFilePath.substring(baseDirPath.length() + 1); // skip file separator
            }
            setKeyFilePath(keyFilePath);
        }
    }//GEN-LAST:event_keyFileChooserActionPerformed

    private void keyFilePanelAncestorWindowShown(de.schlichtherle.truezip.swing.event.PanelEvent evt) {//GEN-FIRST:event_keyFilePanelAncestorWindowShown
        // These are the things I hate Swing for: All I want to do here is to
        // set the focus to the passwd field in this panel when it shows.
        // However, this can't be done in the constructor since the panel is
        // not yet placed in a window which is actually showing.
        // Right, then we use this event listener to do it. This listener
        // method is called when the ancestor window is showing (and coding
        // the event generation was a less than trivial task).
        // But wait, simply setting the focus in this event listener here is
        // not possible on Linux because the containing window (now we have
        // one) didn't gain the focus yet.
        // Strangely enough, this works on Windows.
        // Even more strange, not even calling passwd.requestFocus() makes it
        // work on Linux!
        // So we add a window focus listener here and remove it when we
        // receive a Focus Gained Event.
        // But wait, then we still can't request the focus: This time it
        // doesn't work on Windows, while it works on Linux.
        // I still don't know the reason why, but it seems we're moving too
        // fast, so I have to post a new event to the event queue which finally
        // sets the focus.
        // But wait, requesting the focus could still fail for some strange,
        // undocumented reason - I wouldn't be surprised anymore.
        // So we add a conditional to select the entire contents of the field
        // only if we can really transfer the focus to it.
        // Otherwise, users could get easily confused.
        // If you carefully read the documentation for requestFocusInWindow()
        // however, then you know that even if it returns true, there is still
        // no guarantee that the focus gets actually transferred...
        // This mess is insane (and I can hardly abstain from writing down
        // all the other insulting scatology which comes to my mind)!
        final Window window = evt.getAncestorWindow();
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
    private final javax.swing.JComboBox keyFile = new javax.swing.JComboBox();
    private final javax.swing.JButton keyFileChooser = new javax.swing.JButton();
    private final de.schlichtherle.truezip.swing.EnhancedPanel keyFilePanel = new de.schlichtherle.truezip.swing.EnhancedPanel();
    private javax.swing.JTabbedPane tabs;
    // End of variables declaration//GEN-END:variables

    private static class CustomFileChooser extends javax.swing.JFileChooser {
        private static java.io.File lastCurrentDir = BASE_DIR;
        private static final long serialVersionUID = 2361832976537648223L;
        
        public CustomFileChooser() {
            super(lastCurrentDir);

            setDialogTitle(resources.getString("fileChooser.title"));
            setFileHidingEnabled(false);
        }

        @Override
        public void setCurrentDirectory(java.io.File dir) {
            super.setCurrentDirectory(dir);
            lastCurrentDir = dir;
        }

        @Override
        public java.io.File getCurrentDirectory() {
            return lastCurrentDir = super.getCurrentDirectory();
        }
    }
}
