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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * This panel prompts the user for a key to open an existing protected
 * resource.
 * It currently supports password and key file authentication.
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
public class ReadKeyPanel extends KeyPanel {

    private static final long serialVersionUID = 984673974236493651L;
    private static final String CLASS_NAME = ReadKeyPanel.class.getName();
    private static final ResourceBundle
            resources = ResourceBundle.getBundle(CLASS_NAME);

    private final Color defaultForeground;
    
    /** Constructs a new read key panel. */
    public ReadKeyPanel() {
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

    @Override
    public URI getResource() {
        return URI.create(resource.getText());
    }

    @Override
    public void setResource(final URI resource) {
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

    @Override
    public String getError() {
        final String error = this.error.getText();
        return error.isEmpty() ? null : error;
    }

    @Override
    public void setError(final String error) {
        this.error.setText(error);
    }

    @Override
    boolean updateParam(final AesCipherParameters param) {
        switch (authenticationPanel.getAuthenticationMethod()) {
            case AuthenticationPanel.AUTH_PASSWD:
                param.setPassword(passwd.getPassword());
                return true;

            case AuthenticationPanel.AUTH_KEY_FILE:
                final File keyFile = authenticationPanel.getKeyFile();
                try {
                    param.setKeyFileBytes(AesCipherParametersView.readKeyFile(
                            keyFile));
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
    public boolean isChangeKeySelected() {
        return changeKey.isSelected();
    }

    /**
     * Setter for property changeKeySelected.
     * 
     * @param changeKeySelected New value of property changeKeySelected.
     */
    public void setChangeKeySelected(boolean changeKeySelected) {
        this.changeKey.setSelected(changeKeySelected);
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
            public void ancestorWindowShown(de.schlichtherle.truezip.swing.PanelEvent evt) {
                passwdPanelAncestorWindowShown(evt);
            }
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
        resource.setFont(resource.getFont().deriveFont(resource.getFont().getStyle() | java.awt.Font.BOLD));
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
        error.setName("error"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(15, 0, 0, 0);
        add(error, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents

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
    de.schlichtherle.truezip.crypto.raes.param.swing.AuthenticationPanel authenticationPanel;
    private final javax.swing.JCheckBox changeKey = new javax.swing.JCheckBox();
    javax.swing.JLabel error;
    javax.swing.JPasswordField passwd;
    javax.swing.JLabel passwdLabel;
    de.schlichtherle.truezip.swing.EnhancedPanel passwdPanel;
    javax.swing.JTextPane resource;
    // End of variables declaration//GEN-END:variables
}
