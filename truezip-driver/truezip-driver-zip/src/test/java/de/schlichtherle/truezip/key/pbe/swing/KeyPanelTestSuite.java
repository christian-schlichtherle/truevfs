/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.key.pbe.swing;

import de.schlichtherle.truezip.key.pbe.SafePbeParameters;
import static de.schlichtherle.truezip.swing.JemmyUtils.showFrameWith;
import java.io.File;
import java.net.URI;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.netbeans.jemmy.ComponentChooser;
import org.netbeans.jemmy.QueueTool;
import org.netbeans.jemmy.operators.*;
import org.netbeans.jemmy.util.NameComponentChooser;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public abstract class KeyPanelTestSuite<P extends KeyPanel> {
    private static final ComponentChooser
            KEY_FILE_CHOOSER = new NameComponentChooser("keyFileChooser");
	
    protected P panel;
    protected JFrameOperator frame;
    protected JLabelOperator error;

    @Before
    public void setUp() throws Exception {
        panel = newKeyPanel();
        frame = showFrameWith(panel);
        error = findErrorLabel(frame);
    }

    protected abstract P newKeyPanel();

    protected abstract SafePbeParameters<?, ?> newPbeParameters();

    private JLabelOperator findErrorLabel(final JFrameOperator frame) {
        final String error = "error";
        panel.setError(error);
        final JLabelOperator errorLabel = new JLabelOperator(frame, error);
        frame.pack();
        new QueueTool().waitEmpty();
        panel.setError(null);
        return errorLabel;
    }

    @After
    public void tearDown() throws Exception {
        frame.dispose();
    }

    @Test
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testResource() {
        final URI id = URI.create("HelloWorld!");
        panel.setResource(id);
        assertEquals(id, panel.getResource());

        new JTextComponentOperator(frame, id.toString());
    }

    @Test
    public void testError() {
        panel.setError("This is a test error message!");
        assertFalse(isBlank(error.getText()));
        new JTextFieldOperator(frame).typeText("secret");
        new QueueTool().waitEmpty();
        assertTrue(isBlank(error.getText()));

        panel.setError("This is a test error message!");
        assertFalse(isBlank(error.getText()));
        new JTabbedPaneOperator(frame).selectPage(AuthenticationPanel.AUTH_KEY_FILE); // select tab for key files
        new JButtonOperator(frame, KEY_FILE_CHOOSER).push(); // open file chooser
        new FileChooserOperator().chooseFile("file");
        new QueueTool().waitEmpty();
        assertTrue(isBlank(error.getText()));
    }

    protected static boolean isBlank(String s) {
        return null == s || s.trim().length() <= 0;
    }

    @Test
    public void testKeyFile() {
        final SafePbeParameters<?, ?> param = newPbeParameters();

        new JTabbedPaneOperator(frame).selectPage(AuthenticationPanel.AUTH_KEY_FILE); // select tab for key files

        new JButtonOperator(frame, KEY_FILE_CHOOSER).push(); // open file chooser
        new FileChooserOperator().chooseFile("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!\"$%&/()=?");
        new QueueTool().waitEmpty();
        assertTrue(isBlank(error.getText()));
        assertFalse(panel.updateParam(param));
        assertNotNull(error.getText());

        new JButtonOperator(frame, KEY_FILE_CHOOSER).push(); // open file chooser
        JFileChooserOperator fc = new FileChooserOperator();
        File[] files = fc.getFiles();
        fc.cancel(); // close file chooser

        for (int i = 0, l = files.length; i < l; i++) {
            final File file = files[i];
            if (!file.isFile())
                continue;

            new JButtonOperator(frame, KEY_FILE_CHOOSER).push(); // open file chooser
            fc = new FileChooserOperator();
            fc.setSelectedFile(file);
            fc.approve(); // close file chooser
            new QueueTool().waitEmpty();
            if (panel.updateParam(param)) {
                assertNotNull(param.getPassword());
                assertTrue(isBlank(error.getText()));
            } else {
                assertFalse(isBlank(error.getText()));
            }
        }
    }
}
