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

import java.awt.Component;
import java.awt.Container;
import java.util.Random;
import javax.swing.JPasswordField;
import junit.framework.Assert;
import org.netbeans.jemmy.ComponentChooser;
import org.netbeans.jemmy.QueueTool;
import org.netbeans.jemmy.operators.ContainerOperator;
import org.netbeans.jemmy.operators.JButtonOperator;
import org.netbeans.jemmy.operators.JCheckBoxOperator;
import org.netbeans.jemmy.operators.JDialogOperator;
import org.netbeans.jemmy.operators.JDialogOperator.JDialogFinder;
import org.netbeans.jemmy.operators.JLabelOperator;
import org.netbeans.jemmy.operators.JPasswordFieldOperator;
import org.netbeans.jemmy.operators.JTextComponentOperator;
import org.netbeans.jemmy.operators.Operator;
import org.netbeans.jemmy.util.NameComponentChooser;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 * @since TrueZIP 6.1
 */
public class RemoteControl extends Assert implements Runnable {
    private static int counter;

    private final int index;

    private static final Random rnd = new Random();

    /** The identifier of the protected resource. */
    public final String id;

    /**
     * Contains non-{@code null} if and only if {@code run()} has
     * terminated because an assertion error happened.
     */
    public Throwable throwable;

    public RemoteControl(final String id) {
        this.index = counter++;
        this.id = id;
    }

    public void run() {
        try {
            runIt();
        } catch (Throwable t) {
            throwable = t;
        }
    }

    @SuppressWarnings("deprecation")
    private void runIt() throws AssertionError {
        JDialogOperator dialog;
        JPasswordFieldOperator passwd1,  passwd2;
        String passwd;
        JCheckBoxOperator newPasswd;

        dialog = waitCreateKeyDialog();

        // Check no error message.
        assertNull(findErrorMessage(dialog));

        createResourceHook(dialog);

        // Enter mismatching passwords.
        passwd1 = new JPasswordFieldOperator(dialog, 0);
        passwd2 = new JPasswordFieldOperator(dialog, 1);
        passwd1.typeText("foobar1");
        passwd2.clearText();
        passwd2.typeText("foobar2");

        pushDefaultButton(dialog);

        dialog = waitCreateKeyDialog();

        // Check error message.
        assertNotNull(findErrorMessage(dialog));
        
        // Enter new password twice.
        passwd = "secret " + index;
        passwd1 = new JPasswordFieldOperator(dialog, 0);
        passwd2 = new JPasswordFieldOperator(dialog, 1);
        passwd1.typeText(passwd);

        // Check no error message anymore.
        assertNull(findErrorMessage(dialog));

        passwd2.clearText();
        passwd2.typeText(passwd);

        pushDefaultButton(dialog);

        dialog = waitOpenKeyDialog();

        // Check no error message.
        assertNull(findErrorMessage(dialog));

        // Check no new password requested.
        newPasswd = new JCheckBoxOperator(dialog);
        assertFalse(newPasswd.isSelected());

        // Request new password.
        newPasswd.push();

        // Enter wrong password.
        passwd1 = new JPasswordFieldOperator(dialog, 0);
        passwd2 = null;
        passwd1.clearText();
        passwd1.typeText("wrong");

        pushDefaultButton(dialog);

        dialog = waitOpenKeyDialog();

        // Check error message.
        assertNotNull(findErrorMessage(dialog));

        // Type correct password.
        passwd1 = new JPasswordFieldOperator(dialog, 0);
        passwd2 = null;
        passwd1.clearText();
        passwd1.typeText(passwd);

        // Check no error message anymore.
        assertNull(findErrorMessage(dialog));

        // Check still new password requested.
        newPasswd = new JCheckBoxOperator(dialog);
        assertTrue(newPasswd.isSelected());

        pushDefaultButton(dialog);

        dialog = waitCreateKeyDialog();

        // Check no error message.
        assertNull(findErrorMessage(dialog));

        overwriteResourceHook(dialog);

        // Enter too short passwords.
        passwd1 = new JPasswordFieldOperator(dialog, 0);
        passwd2 = new JPasswordFieldOperator(dialog, 1);
        passwd1.typeText("short"); // 5 chars is too short - min 6!
        passwd2.clearText();
        passwd2.typeText("short");

        pushDefaultButton(dialog);

        dialog = waitCreateKeyDialog();

        // Check error message.
        assertNotNull(findErrorMessage(dialog));

        if (0 == rnd.nextInt(2)) {
            // Enter new password twice.
            passwd = "top secret " + index;
            passwd1 = new JPasswordFieldOperator(dialog, 0);
            passwd2 = new JPasswordFieldOperator(dialog, 1);
            passwd1.typeText(passwd);

            // Check no error message anymore.
            assertNull(findErrorMessage(dialog));

            passwd2.clearText();
            passwd2.typeText(passwd);

            pushDefaultButton(dialog);
        } else {
            // Close dialog in order to reuse the old key.
            dialog.close();
        }
    }

    protected void createResourceHook(JDialogOperator dialog) {
    }

    protected void overwriteResourceHook(JDialogOperator dialog) {
    }

    private JDialogOperator waitCreateKeyDialog() {
        final JDialogOperator dialog = waitDialog();
        assertNotNull("Expected Create Key Dialog for \"" + id + "\"",
                findPasswdField(dialog, 1));
        return dialog;
    }

    private JDialogOperator waitOpenKeyDialog() {
        final JDialogOperator dialog = waitDialog();
        assertNull("Expected Open Key Dialog for \"" + id + "\"",
                findPasswdField(dialog, 1));
        return dialog;
    }

    /**
     * Wait until a dialog pops up which contains our resource ID in
     * a text component.
     */
    private JDialogOperator waitDialog() {
        // Wait for dialog (index position 0).
        // Doing this twice seems to help with some timing issues in Jemmy.
        new QueueTool().waitEmpty(); // waitEmpty(100) doesn't always work
        JDialogOperator dialog = new JDialogOperator(new ByResourceIDFinder());
        new QueueTool().waitEmpty(); // waitEmpty(100) doesn't always work
        /*try {
            Thread.sleep(100);
        } catch (InterruptedException ignored) {
        }*/

        // Ensure that no other dialog is showing, i.e. that there is no
        // dialog at index position 1.
        assertNull(JDialogOperator.findJDialog(
                    new JDialogFinder(),
                    1));

        return dialog;
    }

    private static JPasswordFieldOperator findPasswdField(final JDialogOperator dialog, final int i) {
        final JPasswordField field = JPasswordFieldOperator.findJPasswordField(
                    (Container) dialog.getSource(),
                    new JPasswordFieldOperator.JPasswordFieldFinder(),
                    i);
        return field != null ? new JPasswordFieldOperator(field) : null;
    }

    private class ByResourceIDFinder implements ComponentChooser {
        public boolean checkComponent(Component comp) {
            return null != JTextComponentOperator.findJTextComponent(
                        (Container) comp,
                        new JTextComponentOperator.JTextComponentByTextFinder(
                            id,
                            new Operator.DefaultStringComparator(true, true)));
        }

        public String getDescription() {
            return "Container with Component with exact text \"" + id + "\".";
        }
    }

    private static String findErrorMessage(final ContainerOperator dialog) {
        final JLabelOperator errorLabel
                = new JLabelOperator(dialog, new NameComponentChooser("error"));
        if (errorLabel == null)
            return null;
        final String error = errorLabel.getText();
        if (error == null || error.trim().length() <= 0)
            return null;
        return error;
    }

    @SuppressWarnings("SleepWhileHoldingLock")
    private void pushDefaultButton(JDialogOperator dialog) {
        final JButtonOperator ok = new JButtonOperator(
                dialog.getRootPane().getDefaultButton());
        while (!ok.isEnabled()) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
            }
        }
        ok.push();
    }
}