/*
 * Copyright (C) 2006-2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.crypto.raes.param.swing;

import de.schlichtherle.truezip.io.swing.FileComboBoxBrowser;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.awt.EventQueue;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.io.File;
import java.io.IOException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

/**
 * A panel displaying a password panel or a key file panel in order to let
 * the user select an authentication method and enter a key.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
final class AuthenticationPanel extends JPanel {

    private static final String CLASS_NAME = AuthenticationPanel.class.getName();
    private static final ResourceBundle
            resources = ResourceBundle.getBundle(CLASS_NAME);
    private static final File BASE_DIR = new File(".");

    /** The password authentication method. */
    static final int AUTH_PASSWD = 0;
    
    /** The key file authentication method. */
    static final int AUTH_KEY_FILE = 1;
    private static final long serialVersionUID = 3876515923659236921L;

    /**
     * Creates a new authentication panel.
     * This version of the constructor does not remember the key file path.
     */
    AuthenticationPanel() {
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
    void setPasswdPanel(JPanel passwdPanel) {
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
    File getKeyFile() {
        return new File((String) keyFile.getSelectedItem());
    }

    private void setKeyFile(final File file) {
        final String oldPath = (String) keyFile.getSelectedItem();
        if (file.getPath().equals(oldPath))
            return;
        keyFile.setSelectedItem(file.getPath());
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

        tabs = new javax.swing.JTabbedPane();
        final javax.swing.JLabel keyFileLabel = new javax.swing.JLabel();

        setLayout(new java.awt.GridBagLayout());

        keyFilePanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        keyFilePanel.addPanelListener(new de.schlichtherle.truezip.swing.PanelListener() {
            public void ancestorWindowShown(de.schlichtherle.truezip.swing.PanelEvent evt) {
                keyFilePanelAncestorWindowShown(evt);
            }
            public void ancestorWindowHidden(de.schlichtherle.truezip.swing.PanelEvent evt) {
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
        final JFileChooser fc = new CustomFileChooser();
        if (JFileChooser.APPROVE_OPTION == fc.showOpenDialog(this)) {
            File file = fc.getSelectedFile();
            try {
                final String filePath = file.getCanonicalPath();
                final String baseDirPath = BASE_DIR.getCanonicalPath();
                if (filePath.startsWith(baseDirPath))
                    file = new File(filePath.substring(baseDirPath.length() + 1)); // cut off file separator, too.
                setKeyFile(file);
            } catch (IOException ex) {
                Logger  .getLogger(CLASS_NAME)
                        .log(Level.WARNING, ex.getLocalizedMessage(), ex);
            }
        }
    }//GEN-LAST:event_keyFileChooserActionPerformed

    private void keyFilePanelAncestorWindowShown(de.schlichtherle.truezip.swing.PanelEvent evt) {//GEN-FIRST:event_keyFilePanelAncestorWindowShown
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
    private final javax.swing.JComboBox keyFile = new javax.swing.JComboBox();
    private final javax.swing.JButton keyFileChooser = new javax.swing.JButton();
    private final de.schlichtherle.truezip.swing.EnhancedPanel keyFilePanel = new de.schlichtherle.truezip.swing.EnhancedPanel();
    private javax.swing.JTabbedPane tabs;
    // End of variables declaration//GEN-END:variables

    /** A file chooser which with a dialog title and disabled file hiding. */
    private static class CustomFileChooser extends JFileChooser {
        private static final long serialVersionUID = 2361832976537648223L;
        
        public CustomFileChooser() {
            super(BASE_DIR);
            setDialogTitle(resources.getString("fileChooser.title"));
            setFileHidingEnabled(false);
        }
    }
}
