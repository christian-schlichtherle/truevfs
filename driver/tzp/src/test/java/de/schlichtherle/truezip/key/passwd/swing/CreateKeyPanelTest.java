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

import java.awt.EventQueue;
import java.io.File;
import java.net.URI;
import java.util.Arrays;
import javax.swing.JComponent;
import javax.swing.JFrame;
import junit.framework.TestCase;
import org.netbeans.jemmy.ComponentChooser;
import org.netbeans.jemmy.JemmyProperties;
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

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class CreateKeyPanelTest extends TestCase {
    static {
        JemmyProperties.setCurrentOutput(TestOut.getNullOutput()); // shut up!
    }

    private CreateKeyPanel instance;
    private JFrameOperator frame;
    private JLabelOperator errorLabel;
    private final ComponentChooser keyFileChooser
                = new NameComponentChooser("keyFileChooser");

    public CreateKeyPanelTest(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() throws Exception {
        instance = new CreateKeyPanel();
        frame = showInstanceInFrame();
        errorLabel = findErrorLabel(frame);
    }

    private JFrameOperator showInstanceInFrame() {
        EventQueue.invokeLater(new Runnable() {
            @Override
			public void run() {
                final JFrame frame = new JFrame();
                frame.getContentPane().add(instance);
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        });
        return new JFrameOperator();
    }

    private JLabelOperator findErrorLabel(final JFrameOperator frame) {
        final String error = "error";
        instance.setError(error);
        final JLabelOperator errorLabel = new JLabelOperator(frame, error);
        ((JFrame) frame.getSource()).pack();
        instance.setError(null);
        return errorLabel;
    }

    @Override
    protected void tearDown() throws Exception {
        frame.setVisible(false);
        //frame.dispose();
    }

    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testResourceID() {
        final URI id = URI.create("HelloWorld!");
        instance.setResource(id);
        assertEquals(id, instance.getResource());

        new JTextComponentOperator(frame, id.toString());
    }

    /**
     * Test of setError method, of class de.schlichtherle.truezip.key.passwd.swing.CreateKeyPanel.
     */
    public void testSetError() {
        instance.setError("This is a test error message!");
        assertFalse(isBlank(errorLabel.getText()));
        new JTextFieldOperator(frame).typeText("secret");
        assertTrue(isBlank(errorLabel.getText()));

        instance.setError("This is a test error message!");
        assertFalse(isBlank(errorLabel.getText()));
        new JTabbedPaneOperator(frame).selectPage(AuthenticationPanel.AUTH_KEY_FILE); // select tab for key files
        new JButtonOperator(frame, keyFileChooser).push(); // open file chooser
        new JFileChooserOperator().chooseFile("file");
        assertTrue(isBlank(errorLabel.getText()));
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().length() <= 0;
    }

    /**
     * Test of getCreateKey method, of class de.schlichtherle.truezip.key.passwd.swing.CreateKeyPanel.
     */
    public void testPasswd() {
        String passwd;
        Object result;

        // Check default.
        result = instance.getCreateKey();
        assertNull(result);
        assertFalse(isBlank(errorLabel.getText()));

        // Enter mismatching passwords.
        new JPasswordFieldOperator(frame, 0).setText("foofoo");
        new JPasswordFieldOperator(frame, 1).setText("barbar");
        result = instance.getCreateKey();
        assertNull(result);
        assertFalse(isBlank(errorLabel.getText()));

        // Enter matching passwords, too short.
        passwd = "secre"; // 5 chars is too short
        new JPasswordFieldOperator(frame, 0).setText(passwd);
        new JPasswordFieldOperator(frame, 1).setText(passwd);
        result = instance.getCreateKey();
        assertNull(result);
        assertFalse(isBlank(errorLabel.getText()));

        // Enter matching passwords, long enough.
        passwd = "secret"; // min 6 chars is OK
        new JPasswordFieldOperator(frame, 0).setText(passwd);
        new JPasswordFieldOperator(frame, 1).setText(passwd);
        result = instance.getCreateKey();
        assertTrue(result instanceof char[]);
        assertTrue(Arrays.equals(passwd.toCharArray(), (char[]) result));
        assertTrue(isBlank(errorLabel.getText()));
    }

    /**
     * Test of getCreateKey method, of class de.schlichtherle.truezip.key.passwd.swing.CreateKeyPanel.
     */
    public void testGetFileAsCreateKey() {
        new JTabbedPaneOperator(frame).selectPage(AuthenticationPanel.AUTH_KEY_FILE); // select tab for key files

        new JButtonOperator(frame, keyFileChooser).push(); // open file chooser
        new JFileChooserOperator().chooseFile("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!\"$%&/()=?");
        assertTrue(isBlank(errorLabel.getText()));
        assertNull(instance.getCreateKey());
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
            final Object key = instance.getCreateKey();
            if (key != null) {
                assertTrue(key instanceof byte[]);
                assertTrue(isBlank(errorLabel.getText()));
            } else {
                assertFalse(isBlank(errorLabel.getText()));
            }
        }
    }

    /**
     * Test of get/setExtraDataUI method, of class de.schlichtherle.truezip.key.passwd.swing.CreateKeyPanel.
     */
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testExtraDataUI() {
        final JComponent ui = new AesKeyStrengthPanel();
        instance.setExtraDataUI(ui);
        frame.pack();
        assertSame(ui, instance.getExtraDataUI());

        new JComboBoxOperator(frame); // find combo box
    }
}
