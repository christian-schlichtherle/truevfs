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

import de.schlichtherle.truezip.swing.EnhancedPanel;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.zip.Deflater;
import javax.swing.JComponent;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * This panel prompts the user for a key to create or overwrite a protected
 * resource.
 * It currently supports password and key file authentication, but is
 * extensible for use with certificate based authentication, too.
 *
 * @author Christian Schlichtherle
 * @since TrueZIP 6.0
 * @version $Id$
 */
public class CreateKeyPanel extends EnhancedPanel {

    private static final String CLASS_NAME
            = "de.schlichtherle.truezip.key.passwd.swing.CreateKeyPanel";
    private static final ResourceBundle resources
            = ResourceBundle.getBundle(CLASS_NAME);

    /** The minimum acceptable length of a password. */
    private static final int MIN_PASSWD_LEN = 6;
    
    private static final long serialVersionUID = 6416529465492387235L;

    private final Color defaultForeground;

    private JComponent extraDataUI;

    private Feedback feedback;

    /**
     * Creates new form CreateKeyPanel
     */
    public CreateKeyPanel() {
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
        newPasswd1.getDocument().addDocumentListener(dl);
        newPasswd2.getDocument().addDocumentListener(dl);
        authenticationPanel.getKeyFileDocument().addDocumentListener(dl);
        defaultForeground = resourceID.getForeground();
    }

    private Font getBoldFont() {
        Font font = resourceID.getFont();
        return new Font(font.getName(), Font.BOLD, font.getSize());
    }

    /**
     * Getter for property {@code resourceID}.
     *
     * @return Value of property {@code resourceID}.
     */
    public String getResourceID() {
        return resourceID.getText();
    }
    
    /**
     * Setter for property {@code resourceID}.
     *
     * @param resourceID New value of property {@code resourceID}.
     */
    public void setResourceID(final String resourceID) {
        final String lastResourceID = PromptingKeyProviderUI.lastResourceID;
        assert lastResourceID != null : "violation of contract in PromptingKeyProvider";
        if (!lastResourceID.equals(resourceID) && !"".equals(lastResourceID)) {
            this.resourceID.setForeground(Color.RED);
        } else {
            this.resourceID.setForeground(defaultForeground);
        }
        this.resourceID.setText(resourceID);
        PromptingKeyProviderUI.lastResourceID = resourceID;
    }
    
    /**
     * Getter for property {@code createKey}.
     *
     * @return Value of property {@code createKey}.
     *         This is {@code null} if the user hasn't entered two equal
     *         passwords or if the password is weak.
     *         May also be {@code null} if a key file is selected and
     *         accessing it results in an exception.
     */
    public Cloneable getCreateKey() {
        try {
            switch (authenticationPanel.getAuthenticationMethod()) {
                case AuthenticationPanel.AUTH_PASSWD:
                    final char[] newPasswd1Content = newPasswd1.getPassword();
                    final char[] newPasswd2Content = newPasswd2.getPassword();
                    if (Arrays.equals(newPasswd1Content, newPasswd2Content)) {
                        Arrays.fill(newPasswd2Content, (char) 0);
                        checkPasswdCreateKey(newPasswd1Content);
                        setError(null);
                        return newPasswd1Content;
                    } else {
                        setError(resources.getString("passwd.noMatch"));
                        return null;
                    }

                case AuthenticationPanel.AUTH_KEY_FILE:
                    final String keyFilePathname
                            = authenticationPanel.getKeyFilePath();
                    if (new File(keyFilePathname).canWrite()) {
                        setError(resources.getString("keyFile.canWrite"));
                        return null;
                    }

                    final byte[] key;
                    try {
                        key = PromptingKeyProviderUI.readKeyFile(keyFilePathname);
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
                    checkKeyFileCreateKey(key);
                    setError(null);
                    return key;

                default:
                    throw new AssertionError("Unsupported authentication method!");
            }
        } catch (WeakKeyException failure) {
            setError(failure.getLocalizedMessage());
            return null;
        }
    }

    /** Check the data entropy in the new key. */
    protected void checkKeyFileCreateKey(byte[] createKey)
    throws WeakKeyException {
        Deflater def = new Deflater();
        def.setInput(createKey);
        def.finish();
        assert def.getTotalOut() == 0;
        final int n = def.deflate(new byte[createKey.length * 2]);
        assert def.getTotalOut() == n;
        def.end();
        if (n < 2 * 256 / 8) // see RandomAccessEncryptionSpecification
            throw new WeakKeyException(
                    localizedMessage(resources, "keyFile.badEntropy", null));
    }

    protected void checkPasswdCreateKey(char[] createKey)
    throws WeakKeyException {
        if (createKey.length < MIN_PASSWD_LEN)
            throw new WeakKeyException(localizedMessage(
                    resources, "passwd.tooShort", MIN_PASSWD_LEN));
    }

    private static String localizedMessage(
            final ResourceBundle resources,
            final String key,
            final Object param) {
        return param != null
                ? MessageFormat.format(resources.getString(key), new Object[] { param })
                : resources.getString(key);
    }

    /**
     * Getter for property {@code error}.
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
        // If null is set, the layout seems to ignore the width = 1.0
        // constraint for the component.
        this.error.setText(error != null ? error : " ");
    }
    
    /**
     * Getter for property {@code extraDataUI}.
     * 
     * @return Value of property {@code extraDataUI}.
     */
    public JComponent getExtraDataUI() {
        return extraDataUI;
    }
    
    /**
     * Setter for property {@code extraDataUI}.
     * This component is placed below the two password fields and above the
     * error label.
     * It may be used to prompt the user for additional data which may form
     * part of the key or is separately stored in the key provider.
     * The panel is automatically revalidated.
     * 
     * @param extraDataUI New value of property {@code extraDataUI}.
     */
    public void setExtraDataUI(final JComponent extraDataUI) {
        if (this.extraDataUI == extraDataUI)
            return;

        if (this.extraDataUI != null) {
            remove(this.extraDataUI);
        }
        if (extraDataUI != null) {
            GridBagConstraints gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 3;
            gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints.insets = new Insets(15, 0, 0, 0);
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

        newPasswdPanel = new de.schlichtherle.truezip.swing.EnhancedPanel();
        newPasswd1Label = new javax.swing.JLabel();
        newPasswd1 = new javax.swing.JPasswordField();
        newPasswd2Label = new javax.swing.JLabel();
        newPasswd2 = new javax.swing.JPasswordField();
        final javax.swing.JLabel prompt = new javax.swing.JLabel();
        resourceID = new javax.swing.JTextPane();
        authenticationPanel = new de.schlichtherle.truezip.key.passwd.swing.AuthenticationPanel();
        error = new javax.swing.JLabel();

        newPasswdPanel.setLayout(new java.awt.GridBagLayout());

        newPasswdPanel.addPanelListener(new de.schlichtherle.truezip.swing.event.PanelListener() {
            public void ancestorWindowShown(de.schlichtherle.truezip.swing.event.PanelEvent evt) {
                newPasswdPanelAncestorWindowShown(evt);
            }
            public void ancestorWindowHidden(de.schlichtherle.truezip.swing.event.PanelEvent evt) {
            }
        });

        newPasswd1Label.setDisplayedMnemonic(resources.getString("newPasswd1").charAt(0));
        newPasswd1Label.setLabelFor(newPasswd1);
        newPasswd1Label.setText(resources.getString("newPasswd1")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 5);
        newPasswdPanel.add(newPasswd1Label, gridBagConstraints);

        newPasswd1.setColumns(20);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
        newPasswdPanel.add(newPasswd1, gridBagConstraints);

        newPasswd2Label.setDisplayedMnemonic(resources.getString("newPasswd2").charAt(0));
        newPasswd2Label.setLabelFor(newPasswd2);
        newPasswd2Label.setText(resources.getString("newPasswd2")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 5);
        newPasswdPanel.add(newPasswd2Label, gridBagConstraints);

        newPasswd2.setColumns(20);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        newPasswdPanel.add(newPasswd2, gridBagConstraints);

        setLayout(new java.awt.GridBagLayout());

        addPanelListener(new de.schlichtherle.truezip.swing.event.PanelListener() {
            public void ancestorWindowShown(de.schlichtherle.truezip.swing.event.PanelEvent evt) {
                formAncestorWindowShown(evt);
            }
            public void ancestorWindowHidden(de.schlichtherle.truezip.swing.event.PanelEvent evt) {
            }
        });

        prompt.setLabelFor(resourceID);
        prompt.setText(resources.getString("prompt")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
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
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 15, 0);
        add(resourceID, gridBagConstraints);

        authenticationPanel.setPasswdPanel(newPasswdPanel);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        add(authenticationPanel, gridBagConstraints);

        error.setForeground(new java.awt.Color(255, 0, 0));
        error.setText(" ");
        error.setName("error");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(15, 0, 0, 0);
        add(error, gridBagConstraints);

    }// </editor-fold>//GEN-END:initComponents

    private void formAncestorWindowShown(de.schlichtherle.truezip.swing.event.PanelEvent evt) {//GEN-FIRST:event_formAncestorWindowShown
        final Feedback feedback = getFeedback();
        if (feedback != null) {
            feedback.setPanel(this);
            feedback.run();
        }
    }//GEN-LAST:event_formAncestorWindowShown

    private void newPasswdPanelAncestorWindowShown(de.schlichtherle.truezip.swing.event.PanelEvent evt) {//GEN-FIRST:event_newPasswdPanelAncestorWindowShown
        // These are the things I hate Swing for: All I want to do here is to
        // set the focus to the newPasswd1 field in this panel when it shows.
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
        assert window != null : "illegal state";
        window.addWindowFocusListener(new WindowFocusListener() {
            public void windowGainedFocus(WindowEvent e) {
                window.removeWindowFocusListener(this);
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        if (newPasswd1.requestFocusInWindow()) {
                            newPasswd1.selectAll();
                            newPasswd2.selectAll();
                        }
                    }
                });
            }

            public void windowLostFocus(WindowEvent e) {
            }
        });
    }//GEN-LAST:event_newPasswdPanelAncestorWindowShown
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private de.schlichtherle.truezip.key.passwd.swing.AuthenticationPanel authenticationPanel;
    private javax.swing.JLabel error;
    private javax.swing.JPasswordField newPasswd1;
    private javax.swing.JLabel newPasswd1Label;
    private javax.swing.JPasswordField newPasswd2;
    private javax.swing.JLabel newPasswd2Label;
    private de.schlichtherle.truezip.swing.EnhancedPanel newPasswdPanel;
    private javax.swing.JTextPane resourceID;
    // End of variables declaration//GEN-END:variables
}
