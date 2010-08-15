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

package de.schlichtherle.key.passwd.swing;

import de.schlichtherle.swing.EnhancedPanel;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;

/**
 * This panel prompts the user for a key to open an existing protected
 * resource.
 * It currently supports password and key file authentication, but is
 * extensible for use with certificate based authentication, too.
 * <p>
 * Note that the contents of the password and file path fields are stored in
 * a static field from which they are restored when a new panel is created.
 * This is very convenient for the user if she inadvertently entered a wrong
 * key or shares the same key for multiple protected resources.
 *
 * @author Christian Schlichtherle
 * @since TrueZIP 6.0
 * @version $Revision$
 */
public class OpenKeyPanel extends EnhancedPanel {

    private static final String CLASS_NAME
            = "de/schlichtherle/key/passwd/swing/OpenKeyPanel".replace('/', '.'); // beware of code obfuscation!
    private static final ResourceBundle resources
            = ResourceBundle.getBundle(CLASS_NAME);

    private final Color defaultForeground;

    private JComponent extraDataUI;

    private Feedback feedback;
    
    /**
     * Creates new form OpenKeyPanel
     */
    public OpenKeyPanel() {
        initComponents();
        final DocumentListener dl = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                setError(null);
            }

            public void removeUpdate(DocumentEvent e) {
                setError(null);
            }

            public void changedUpdate(DocumentEvent e) {
                setError(null);
            }
        };
        passwd.getDocument().addDocumentListener(dl);
        authenticationPanel.getKeyFileDocument().addDocumentListener(dl);
        defaultForeground = resourceID.getForeground();
    }

    private Font getBoldFont() {
        return resourceID.getFont().deriveFont(Font.BOLD);
    }

    /**
     * Setter for property <code>resourceID</code>.
     *
     * @param resourceID New value of property <code>resourceID</code>.
     */
    public void setResourceID(final String resourceID) {
        final String lastResourceID = PromptingKeyProviderUI.lastResourceID;
        if (!lastResourceID.equals(resourceID) && !"".equals(lastResourceID)) {
            this.resourceID.setForeground(Color.RED);
        } else {
            this.resourceID.setForeground(defaultForeground);
        }
        this.resourceID.setText(resourceID);
        PromptingKeyProviderUI.lastResourceID = resourceID;
    }

    /**
     * Getter for property <code>resourceID</code>.
     *
     * @return Value of property <code>resourceID</code>.
     */
    public String getResourceID() {
        return resourceID.getText();
    }

    /**
     * Getter for property <code>error</code>.
     */
    public String getError() {
        final String error = this.error.getText();
        return error.trim().length() > 0 ? error : null;
    }
    
    /**
     * Setter for property error.
     *
     * @param error New value of property error.
     */
    public void setError(final String error) {
        // Fix layout issue with GridBagLayout:
        // If null is set, the layout seems to ignore the widthy = 1.0
        // constraint for the component.
        this.error.setText(error != null ? error : " ");
    }

    /**
     * Getter for property <code>openKey</code>.
     * If a key file is selected and an error occurs when accessing it,
     * a descriptive message is set for the <code>error</code> property.
     *
     * @return Value of property <code>openKey</code>.
     *         May be <code>null</code> if a key file is selected and
     *         accessing it results in an exception.
     */
    public Object getOpenKey() {
        switch (authenticationPanel.getAuthenticationMethod()) {
            case AuthenticationPanel.AUTH_PASSWD:
                return passwd.getPassword();

            case AuthenticationPanel.AUTH_KEY_FILE:
                final String keyFilePathname
                        = authenticationPanel.getKeyFilePath();
                try {
                    return PromptingKeyProviderUI.readKeyFile(keyFilePathname);
                } catch (EOFException failure) {
                    setError(resources.getString("keyFile.eofException"));
                    return null;
                } catch (FileNotFoundException failure) {
                    setError(resources.getString("keyFile.fileNotFoundException"));
                    return null;
                } catch (IOException failure) {
                    setError(resources.getString("keyFile.ioException"));
                    return null;
                }

            default:
                throw new AssertionError("Unsupported authentication method!");
        }
    }

    /**
     * Setter for property changeKeyRequested.
     * 
     * @param changeKeyRequested New value of property changeKeyRequested.
     * @deprecated This method should be package private.
     */
    public void setKeyChangeRequested(final boolean changeKeyRequested) {
        this.changeKey.setSelected(changeKeyRequested);
    }

    /**
     * Getter for property changeKeyRequested.
     * 
     * @return Value of property changeKeyRequested.
     */
    public boolean isKeyChangeRequested() {
        return changeKey.isSelected();
    }
    
    /**
     * Getter for property <code>extraDataUI</code>.
     * 
     * @return Value of property <code>extraDataUI</code>.
     */
    public JComponent getExtraDataUI() {
        return extraDataUI;
    }
    
    /**
     * Setter for property <code>extraDataUI</code>.
     * This component is placed below the password field and above the
     * "change password / key file" check box.
     * It may be used to prompt the user for additional data which may form
     * part of the key or is separately stored in the key provider.
     * The panel is automatically revalidated.
     * 
     * @param extraDataUI New value of property <code>extraDataUI</code>.
     */
    public void setExtraDataUI(final JComponent extraDataUI) {
        if (this.extraDataUI == extraDataUI)
            return;

        if (this.extraDataUI != null) {
            remove(this.extraDataUI);
        }
        if (extraDataUI != null) {
            java.awt.GridBagConstraints gridBagConstraints;
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 4;
            gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
            gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
            gridBagConstraints.insets = new java.awt.Insets(15, 0, 0, 0);
            add(extraDataUI, gridBagConstraints);
        }
        this.extraDataUI = extraDataUI;

        revalidate();
    }

    /**
     * Returns the feedback to run when this panel is shown in its ancestor
     * window.
     */
    public Feedback getFeedback() {
        return feedback;
    }

    /**
     * Sets the feedback to run when this panel is shown in its ancestor
     * window.
     */
    public void setFeedback(final Feedback feedback) {
        this.feedback = feedback;
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        passwdPanel = new de.schlichtherle.swing.EnhancedPanel();
        passwdLabel = new javax.swing.JLabel();
        passwd = new javax.swing.JPasswordField();
        final javax.swing.JLabel prompt = new javax.swing.JLabel();
        resourceID = new javax.swing.JTextPane();
        authenticationPanel = new de.schlichtherle.key.passwd.swing.AuthenticationPanel();
        error = new javax.swing.JLabel();

        passwdPanel.setLayout(new java.awt.GridBagLayout());

        passwdPanel.addPanelListener(new de.schlichtherle.swing.event.PanelListener() {
            public void ancestorWindowShown(de.schlichtherle.swing.event.PanelEvent evt) {
                passwdPanelAncestorWindowShown(evt);
            }
            public void ancestorWindowHidden(de.schlichtherle.swing.event.PanelEvent evt) {
            }
        });

        passwdLabel.setDisplayedMnemonic(java.util.ResourceBundle.getBundle("de/schlichtherle/key/passwd/swing/OpenKeyPanel").getString("passwd").charAt(0));
        passwdLabel.setLabelFor(passwd);
        passwdLabel.setText(resources.getString("passwd")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 5);
        passwdPanel.add(passwdLabel, gridBagConstraints);

        passwd.setColumns(20);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        passwdPanel.add(passwd, gridBagConstraints);

        setLayout(new java.awt.GridBagLayout());

        addPanelListener(new de.schlichtherle.swing.event.PanelListener() {
            public void ancestorWindowShown(de.schlichtherle.swing.event.PanelEvent evt) {
                formAncestorWindowShown(evt);
            }
            public void ancestorWindowHidden(de.schlichtherle.swing.event.PanelEvent evt) {
            }
        });

        prompt.setLabelFor(resourceID);
        prompt.setText(resources.getString("prompt")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
        add(prompt, gridBagConstraints);

        resourceID.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createEtchedBorder(), javax.swing.BorderFactory.createEmptyBorder(2, 2, 2, 2)));
        resourceID.setEditable(false);
        resourceID.setFont(getBoldFont());
        resourceID.setOpaque(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 15, 0);
        add(resourceID, gridBagConstraints);

        authenticationPanel.setPasswdPanel(passwdPanel);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        add(authenticationPanel, gridBagConstraints);

        changeKey.setText(resources.getString("changeKey")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(15, 0, 0, 0);
        add(changeKey, gridBagConstraints);

        error.setForeground(java.awt.Color.red);
        error.setText(" ");
        error.setName("error");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(15, 0, 0, 0);
        add(error, gridBagConstraints);

    }// </editor-fold>//GEN-END:initComponents

    private void formAncestorWindowShown(de.schlichtherle.swing.event.PanelEvent evt) {//GEN-FIRST:event_formAncestorWindowShown
        final Feedback feedback = getFeedback();
        if (feedback != null) {
            feedback.setPanel(this);
            feedback.run();
        }
    }//GEN-LAST:event_formAncestorWindowShown

    private void passwdPanelAncestorWindowShown(de.schlichtherle.swing.event.PanelEvent evt) {//GEN-FIRST:event_passwdPanelAncestorWindowShown
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
            public void windowGainedFocus(WindowEvent e) {
                window.removeWindowFocusListener(this);
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        if (passwd.requestFocusInWindow())
                            passwd.selectAll();
                    }
                });
            }

            public void windowLostFocus(WindowEvent e) {
            }
        });
    }//GEN-LAST:event_passwdPanelAncestorWindowShown
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private de.schlichtherle.key.passwd.swing.AuthenticationPanel authenticationPanel;
    private final javax.swing.JCheckBox changeKey = new javax.swing.JCheckBox();
    private javax.swing.JLabel error;
    private javax.swing.JPasswordField passwd;
    private javax.swing.JLabel passwdLabel;
    private de.schlichtherle.swing.EnhancedPanel passwdPanel;
    private javax.swing.JTextPane resourceID;
    // End of variables declaration//GEN-END:variables
}
