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

import de.schlichtherle.truezip.crypto.raes.param.AesCipherParameters;
import de.schlichtherle.truezip.swing.EnhancedPanel;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.ResourceBundle;
import javax.swing.JComponent;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

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
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
final class OpenKeyPanel extends EnhancedPanel {

    private static final String CLASS_NAME = OpenKeyPanel.class.getName();
    private static final ResourceBundle resources
            = ResourceBundle.getBundle(CLASS_NAME);
    private static final long serialVersionUID = 984673974236493651L;

    private final Color defaultForeground;

    private JComponent extraDataUI;

    private Feedback feedback;
    
    /**
     * Creates new form OpenKeyPanel
     */
    public OpenKeyPanel() {
        initComponents();
        final DocumentListener dl = new DocumentListener() {
            @Override
			public void insertUpdate(DocumentEvent e) {
                setError(null);
            }

            @Override
			public void removeUpdate(DocumentEvent e) {
                setError(null);
            }

            @Override
			public void changedUpdate(DocumentEvent e) {
                setError(null);
            }
        };
        passwd.getDocument().addDocumentListener(dl);
        authenticationPanel.getKeyFileDocument().addDocumentListener(dl);
        defaultForeground = resource.getForeground();
    }

    private Font getBoldFont() {
        return resource.getFont().deriveFont(Font.BOLD);
    }

    /**
     * Getter for property {@code resourceID}.
     *
     * @return Value of property {@code resourceID}.
     */
    URI getResource() {
        return URI.create(resource.getText());
    }

    /**
     * Setter for property {@code resourceID}.
     *
     * @param resource New value of property {@code resourceID}.
     */
    void setResource(final URI resource) {
        final URI lastResource = AesCipherParametersView.lastResource;
        if (!lastResource.equals(resource)
                && !lastResource.equals(AesCipherParametersView.INITIAL_RESOURCE)) {
            this.resource.setForeground(Color.RED);
        } else {
            this.resource.setForeground(defaultForeground);
        }
        this.resource.setText(resource.toString());
        AesCipherParametersView.lastResource = resource;
    }

    /**
     * Getter for property {@code error}.
     */
    @CheckForNull String getError() {
        final String error = this.error.getText();
        return error.trim().length() > 0 ? error : null;
    }
    
    /**
     * Setter for property error.
     *
     * @param error New value of property error.
     */
    void setError(final @CheckForNull String error) {
        // Fix layout issue with GridBagLayout:
        // If null is set, the layout seems to ignore the widthy = 1.0
        // constraint for the component.
        this.error.setText(error != null ? error : " ");
    }

    /**
     * Getter for property {@code openKey}.
     * If a key file is selected and an error occurs when accessing it,
     * a descriptive message is set for the {@code error} property.
     *
     * @return Value of property {@code openKey}.
     *         May be {@code null} if a key file is selected and
     *         accessing it results in an exception.
     */
    boolean updateOpenKey(final AesCipherParameters param) {
        switch (authenticationPanel.getAuthenticationMethod()) {
            case AuthenticationPanel.AUTH_PASSWD:
                param.setPassword(passwd.getPassword());
                return true;

            case AuthenticationPanel.AUTH_KEY_FILE:
                final File keyFile = authenticationPanel.getKeyFile();
                try {
                    param.setKeyFileBytes(AesCipherParametersView.readKeyFile(keyFile));
                    return true;
                } catch (EOFException ex) {
                    setError(resources.getString("keyFile.eofException"));
                    return false;
                } catch (FileNotFoundException ex) {
                    setError(resources.getString("keyFile.fileNotFoundException"));
                    return false;
                } catch (IOException ex) {
                    setError(resources.getString("keyFile.ioException"));
                    return false;
                }

            default:
                throw new AssertionError("Unsupported authentication method!");
        }
    }

    /**
     * Getter for property changeKeySelected.
     *
     * @return Value of property changeKeySelected.
     */
    boolean isChangeKeySelected() {
        return changeKey.isSelected();
    }

    /**
     * Setter for property changeKeySelected.
     * 
     * @param changeKeySelected New value of property changeKeySelected.
     */
    void setChangeKeySelected(boolean changeKeySelected) {
        this.changeKey.setSelected(changeKeySelected);
    }
    
    /**
     * Getter for property {@code extraDataUI}.
     * 
     * @return Value of property {@code extraDataUI}.
     */
    JComponent getExtraDataUI() {
        return extraDataUI;
    }
    
    /**
     * Setter for property {@code extraDataUI}.
     * This component is placed below the password field and above the
     * "change password / key file" check box.
     * It may be used to prompt the user for additional data which may form
     * part of the key or is separately stored in the key provider.
     * The panel is automatically revalidated.
     * 
     * @param extraDataUI New value of property {@code extraDataUI}.
     */
    void setExtraDataUI(final JComponent extraDataUI) {
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
    Feedback getFeedback() {
        return feedback;
    }

    /**
     * Sets the feedback to run when this panel is shown in its ancestor
     * window.
     */
    void setFeedback(final Feedback feedback) {
        this.feedback = feedback;
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        passwdPanel = new de.schlichtherle.truezip.swing.EnhancedPanel();
        passwdLabel = new javax.swing.JLabel();
        passwd = new javax.swing.JPasswordField();
        final javax.swing.JLabel prompt = new javax.swing.JLabel();
        resource = new javax.swing.JTextPane();
        authenticationPanel = new de.schlichtherle.truezip.crypto.raes.param.swing.AuthenticationPanel();
        error = new javax.swing.JLabel();

        passwdPanel.addPanelListener(new de.schlichtherle.truezip.swing.PanelListener() {
            @Override
			public void ancestorWindowShown(de.schlichtherle.truezip.swing.PanelEvent evt) {
                passwdPanelAncestorWindowShown(evt);
            }
            @Override
			public void ancestorWindowHidden(de.schlichtherle.truezip.swing.PanelEvent evt) {
            }
        });
        passwdPanel.setLayout(new java.awt.GridBagLayout());

        passwdLabel.setDisplayedMnemonic(resources.getString("passwd").charAt(0));
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

        addPanelListener(new de.schlichtherle.truezip.swing.PanelListener() {
            @Override
			public void ancestorWindowShown(de.schlichtherle.truezip.swing.PanelEvent evt) {
                formAncestorWindowShown(evt);
            }
            @Override
			public void ancestorWindowHidden(de.schlichtherle.truezip.swing.PanelEvent evt) {
            }
        });
        setLayout(new java.awt.GridBagLayout());

        prompt.setLabelFor(resource);
        prompt.setText(resources.getString("prompt")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
        add(prompt, gridBagConstraints);

        resource.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createEtchedBorder(), javax.swing.BorderFactory.createEmptyBorder(2, 2, 2, 2)));
        resource.setEditable(false);
        resource.setFont(getBoldFont());
        resource.setOpaque(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 15, 0);
        add(resource, gridBagConstraints);

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
        error.setName("error"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(15, 0, 0, 0);
        add(error, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents

    private void formAncestorWindowShown(de.schlichtherle.truezip.swing.PanelEvent evt) {//GEN-FIRST:event_formAncestorWindowShown
        final Feedback feedback = getFeedback();
        if (null != feedback)
            feedback.feedback(this);
    }//GEN-LAST:event_formAncestorWindowShown

    private void passwdPanelAncestorWindowShown(de.schlichtherle.truezip.swing.PanelEvent evt) {//GEN-FIRST:event_passwdPanelAncestorWindowShown
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
        assert null != window : "illegal state";
        window.addWindowFocusListener(new WindowFocusListener() {
            @Override
			public void windowGainedFocus(WindowEvent e) {
                window.removeWindowFocusListener(this);
                EventQueue.invokeLater(new Runnable() {
                    @Override
					public void run() {
                        if (passwd.requestFocusInWindow())
                            passwd.selectAll();
                    }
                });
            }

            @Override
			public void windowLostFocus(WindowEvent e) {
            }
        });
    }//GEN-LAST:event_passwdPanelAncestorWindowShown
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private de.schlichtherle.truezip.crypto.raes.param.swing.AuthenticationPanel authenticationPanel;
    private final javax.swing.JCheckBox changeKey = new javax.swing.JCheckBox();
    private javax.swing.JLabel error;
    private javax.swing.JPasswordField passwd;
    private javax.swing.JLabel passwdLabel;
    private de.schlichtherle.truezip.swing.EnhancedPanel passwdPanel;
    private javax.swing.JTextPane resource;
    // End of variables declaration//GEN-END:variables
}
