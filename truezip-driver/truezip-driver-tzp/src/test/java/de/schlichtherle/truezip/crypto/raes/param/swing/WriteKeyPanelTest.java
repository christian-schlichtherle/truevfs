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
import java.io.File;
import java.net.URI;
import javax.swing.JComponent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.netbeans.jemmy.ComponentChooser;
import org.netbeans.jemmy.JemmyProperties;
import org.netbeans.jemmy.QueueTool;
import org.netbeans.jemmy.TestOut;
import org.netbeans.jemmy.operators.JButtonOperator;
import org.netbeans.jemmy.operators.JComboBoxOperator;
import org.netbeans.jemmy.operators.JFileChooserOperator;
import org.netbeans.jemmy.operators.JFrameOperator;
import org.netbeans.jemmy.operators.JLabelOperator;
import org.netbeans.jemmy.operators.JPasswordFieldOperator;
import org.netbeans.jemmy.operators.JTabbedPaneOperator;
import org.netbeans.jemmy.operators.JTextComponentOperator;
import org.netbeans.jemmy.operators.JTextFieldOperator;
import org.netbeans.jemmy.util.NameComponentChooser;

import static org.junit.Assert.*;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public final class WriteKeyPanelTest {
    static {
        JemmyProperties.setCurrentOutput(TestOut.getNullOutput()); // shut up!
    }

    private WriteKeyPanel panel;
    private JFrameOperator frame;
    private JLabelOperator errorLabel;
    private final ComponentChooser keyFileChooser
                = new NameComponentChooser("keyFileChooser");

    @Before
    public void setUp() throws Exception {
        panel = new WriteKeyPanel();
        frame = JemmyUtils.showInNewFrame(panel);
        errorLabel = findErrorLabel(frame);
    }

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
    public void testResourceID() {
        final URI id = URI.create("HelloWorld!");
        panel.setResource(id);
        assertEquals(id, panel.getResource());

        new JTextComponentOperator(frame, id.toString());
    }

    @Test
    public void testSetError() {
        panel.setError("This is a test error message!");
        assertFalse(isBlank(errorLabel.getText()));
        new JTextFieldOperator(frame).typeText("secret");
        assertTrue(isBlank(errorLabel.getText()));

        panel.setError("This is a test error message!");
        assertFalse(isBlank(errorLabel.getText()));
        new JTabbedPaneOperator(frame).selectPage(AuthenticationPanel.AUTH_KEY_FILE); // select tab for key files
        new JButtonOperator(frame, keyFileChooser).push(); // open file chooser
        new JFileChooserOperator().chooseFile("file");
        assertTrue(isBlank(errorLabel.getText()));
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().length() <= 0;
    }

    @Test
    public void testPasswd() {
        final AesCipherParameters param = new AesCipherParameters();

        // Check default.
        assertFalse(panel.updateWriteKey(param));
        assertNull(param.getPassword());
        assertFalse(isBlank(errorLabel.getText()));

        // Enter mismatching passwords.
        new JPasswordFieldOperator(frame, 0).setText("foofoo");
        new JPasswordFieldOperator(frame, 1).setText("barbar");
        assertFalse(panel.updateWriteKey(param));
        assertNull(param.getPassword());
        assertFalse(isBlank(errorLabel.getText()));

        // Enter matching passwords, too short.
        String passwd = "secre"; // 5 chars is too short
        new JPasswordFieldOperator(frame, 0).setText(passwd);
        new JPasswordFieldOperator(frame, 1).setText(passwd);
        assertFalse(panel.updateWriteKey(param));
        assertNull(param.getPassword());
        assertFalse(isBlank(errorLabel.getText()));

        // Enter matching passwords, long enough.
        passwd = "secret"; // min 6 chars is OK
        new JPasswordFieldOperator(frame, 0).setText(passwd);
        new JPasswordFieldOperator(frame, 1).setText(passwd);
        assertTrue(panel.updateWriteKey(param));
        assertEquals(passwd, new String(param.getPassword()));
        assertTrue(isBlank(errorLabel.getText()));
    }

    @Test
    public void testKeyFile() {
        final AesCipherParameters param = new AesCipherParameters();

        new JTabbedPaneOperator(frame).selectPage(AuthenticationPanel.AUTH_KEY_FILE); // select tab for key files

        new JButtonOperator(frame, keyFileChooser).push(); // open file chooser
        new JFileChooserOperator().chooseFile("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!\"$%&/()=?");
        assertTrue(isBlank(errorLabel.getText()));
        assertFalse(panel.updateWriteKey(param));
        assertFalse(isBlank(errorLabel.getText()));

        new JButtonOperator(frame, keyFileChooser).push(); // open file chooser
        JFileChooserOperator fc = new JFileChooserOperator();
        File[] files = fc.getFiles();
        fc.cancel(); // revert to password panel

        for (int i = 0, l = files.length; i < l; i++) {
            final File file = files[i];
            if (!file.isFile())
                continue;

            new JButtonOperator(frame, keyFileChooser).push(); // open file chooser
            fc = new JFileChooserOperator();
            fc.setSelectedFile(file);
            fc.approve();
            if (panel.updateWriteKey(param)) {
                assertNotNull(param.getPassword());
                assertTrue(isBlank(errorLabel.getText()));
            } else {
                assertFalse(isBlank(errorLabel.getText()));
            }
        }
    }

    @Test
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testExtraDataUI() {
        final JComponent ui = new AesKeyStrengthPanel();
        panel.setExtraDataUI(ui);
        frame.pack();
        assertSame(ui, panel.getExtraDataUI());

        new JComboBoxOperator(frame); // find combo box
    }
}
