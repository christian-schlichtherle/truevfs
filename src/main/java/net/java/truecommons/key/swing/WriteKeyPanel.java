/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.key.swing;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.java.truecommons.key.spec.prompting.PromptingPbeParameters;

import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.ResourceBundle;

/**
 * This panel prompts the user for a key to create or overwrite a protected
 * resource.
 * It currently supports password and key file authentication.
 *
 * @since  TrueCommons 2.2
 * @author Christian Schlichtherle
 */
final class WriteKeyPanel extends KeyPanel {

    private static final long serialVersionUID = 0L;

    private static final ResourceBundle
            resources = ResourceBundle.getBundle(WriteKeyPanel.class.getName());

    /** The minimum acceptable length of a password. */
    private static final int MIN_PASSWD_LEN = 8;

    private final SwingPromptingPbeParametersView<?, ?> view;
    private final Color defaultForeground;

    private JComponent extraDataUI;

    /** Constructs a new write key panel. */
    WriteKeyPanel(final SwingPromptingPbeParametersView<?, ?> view) {
        assert null != view;
        this.view = view;
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
        newPasswd1Field.getDocument().addDocumentListener(dl);
        newPasswd2Field.getDocument().addDocumentListener(dl);
        authenticationPanel.getKeyFileDocument().addDocumentListener(dl);
        defaultForeground = resource.getForeground();
    }

    @Override
    public URI getResource() {
        return URI.create(resource.getText());
    }

    @Override
    @SuppressFBWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
    public void setResource(final URI resource) {
        final @Nullable URI lastResource = view.getLastResource();
        if (null != lastResource && !resource.equals(lastResource)) {
            this.resource.setForeground(Color.RED);
        } else {
            this.resource.setForeground(defaultForeground);
        }
        this.resource.setText(resource.toString());
        view.setLastResource(resource);
    }

    @Override
    public String getError() {
        final String error = this.error.getText();
        return error.trim().length() > 0 ? error : null;
    }

    @Override
    public void setError(final String error) {
        this.error.setText(error);
    }

    @Override
    void updateParamChecked(final PromptingPbeParameters<?, ?> param)
    throws AuthenticationException {
        switch (authenticationPanel.getAuthenticationMethod()) {
            case AuthenticationPanel.AUTH_PASSWD:
                final char[] newPasswd1 = newPasswd1Field.getPassword();
                final char[] newPasswd2 = newPasswd2Field.getPassword();
                try {
                    if (!Arrays.equals(newPasswd1, newPasswd2))
                        throw new AuthenticationException(
                                resources.getString("passwd.noMatch"));
                    checkPasswdKey(newPasswd1);
                    param.setPassword(newPasswd1);
                } finally {
                    Arrays.fill(newPasswd1, (char) 0);
                    Arrays.fill(newPasswd2, (char) 0);
                }
                break;
            case AuthenticationPanel.AUTH_KEY_FILE:
                final File keyFile = authenticationPanel.getKeyFile();
                SwingPromptingPbeParametersView
                        .setPasswordOn(param, keyFile, true);
                break;
            default:
                throw new AssertionError("Unsupported authentication method!");
        }
    }

    /**
     * Checks the entropy of the given key.
     *
     * @param key the key to check.
     * @throws AuthenticationException if the entropy of the given key is too weak.
     */
    private void checkPasswdKey(char[] key)
    throws AuthenticationException {
        if (MIN_PASSWD_LEN > key.length)
            throw new AuthenticationException(
                    String.format(resources.getString("passwd.tooShort"), MIN_PASSWD_LEN));
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

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        final net.java.truecommons.key.swing.util.EnhancedPanel passwdPanel = new net.java.truecommons.key.swing.util.EnhancedPanel();
        final javax.swing.JLabel newPasswd1Label = new javax.swing.JLabel();
        final javax.swing.JLabel newPasswd2Label = new javax.swing.JLabel();
        final javax.swing.JLabel prompt = new javax.swing.JLabel();

        passwdPanel.addPanelListener(new net.java.truecommons.key.swing.util.PanelListener() {
            public void ancestorWindowShown(net.java.truecommons.key.swing.util.PanelEvent evt) {
                passwdPanelAncestorWindowShown(evt);
            }
            public void ancestorWindowHidden(net.java.truecommons.key.swing.util.PanelEvent evt) {
            }
        });
        passwdPanel.setLayout(new java.awt.GridBagLayout());

        newPasswd1Label.setDisplayedMnemonic(resources.getString("newPasswd1").charAt(0));
        newPasswd1Label.setLabelFor(newPasswd1Field);
        newPasswd1Label.setText(resources.getString("newPasswd1")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 5);
        passwdPanel.add(newPasswd1Label, gridBagConstraints);

        newPasswd1Field.setColumns(20);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
        passwdPanel.add(newPasswd1Field, gridBagConstraints);

        newPasswd2Label.setDisplayedMnemonic(resources.getString("newPasswd2").charAt(0));
        newPasswd2Label.setLabelFor(newPasswd2Field);
        newPasswd2Label.setText(resources.getString("newPasswd2")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 5);
        passwdPanel.add(newPasswd2Label, gridBagConstraints);

        newPasswd2Field.setColumns(20);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        passwdPanel.add(newPasswd2Field, gridBagConstraints);

        setLayout(new java.awt.GridBagLayout());

        prompt.setLabelFor(resource);
        prompt.setText(resources.getString("prompt")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
        add(prompt, gridBagConstraints);

        resource.setEditable(false);
        resource.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createEtchedBorder(), javax.swing.BorderFactory.createEmptyBorder(2, 2, 2, 2)));
        resource.setFont(resource.getFont().deriveFont(resource.getFont().getStyle() | java.awt.Font.BOLD));
        resource.setOpaque(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 15, 0);
        add(resource, gridBagConstraints);

        authenticationPanel.setPasswdPanel(passwdPanel);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        add(authenticationPanel, gridBagConstraints);

        error.setForeground(new java.awt.Color(255, 0, 0));
        error.setName("error"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(15, 0, 0, 0);
        add(error, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents

    private void passwdPanelAncestorWindowShown(net.java.truecommons.key.swing.util.PanelEvent evt) {//GEN-FIRST:event_passwdPanelAncestorWindowShown
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
        final Window window = evt.getSource().getAncestorWindow();
        window.addWindowFocusListener(new WindowFocusListener() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                window.removeWindowFocusListener(this);
                EventQueue.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        if (newPasswd1Field.requestFocusInWindow()) {
                            newPasswd1Field.selectAll();
                            newPasswd2Field.selectAll();
                        }
                    }
                });
            }

            @Override
            public void windowLostFocus(WindowEvent e) {
            }
        });
    }//GEN-LAST:event_passwdPanelAncestorWindowShown

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private final net.java.truecommons.key.swing.AuthenticationPanel authenticationPanel = new net.java.truecommons.key.swing.AuthenticationPanel();
    private final javax.swing.JLabel error = new javax.swing.JLabel();
    private final javax.swing.JPasswordField newPasswd1Field = new javax.swing.JPasswordField();
    private final javax.swing.JPasswordField newPasswd2Field = new javax.swing.JPasswordField();
    private final javax.swing.JTextPane resource = new javax.swing.JTextPane();
    // End of variables declaration//GEN-END:variables
}