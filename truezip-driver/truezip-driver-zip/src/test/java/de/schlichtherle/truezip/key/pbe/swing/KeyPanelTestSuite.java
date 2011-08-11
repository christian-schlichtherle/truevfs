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
package de.schlichtherle.truezip.key.pbe.swing;

import de.schlichtherle.truezip.key.pbe.SafePbeParameters;
import java.io.File;
import java.net.URI;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.netbeans.jemmy.ComponentChooser;
import org.netbeans.jemmy.QueueTool;
import org.netbeans.jemmy.operators.JButtonOperator;
import org.netbeans.jemmy.operators.JFileChooserOperator;
import org.netbeans.jemmy.operators.JFrameOperator;
import org.netbeans.jemmy.operators.JLabelOperator;
import org.netbeans.jemmy.operators.JTabbedPaneOperator;
import org.netbeans.jemmy.operators.JTextComponentOperator;
import org.netbeans.jemmy.operators.JTextFieldOperator;
import org.netbeans.jemmy.util.NameComponentChooser;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public abstract class KeyPanelTestSuite<P extends KeyPanel>
extends JemmyUtils {
    private static final ComponentChooser
            KEY_FILE_CHOOSER = new NameComponentChooser("keyFileChooser");
	
    protected P panel;
    protected JFrameOperator frame;
    protected JLabelOperator error;

    @Before
    public void setUp() throws Exception {
        panel = newKeyPanel();
        frame = showInNewFrame(panel);
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
