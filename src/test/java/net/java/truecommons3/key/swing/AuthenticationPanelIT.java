/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.key.swing;

import java.io.IOException;
import net.java.truecommons3.key.swing.io.JemmyUtilsWithFile;
import net.java.truecommons3.key.swing.util.FileChooserOfWindowOperator;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import org.junit.Ignore;
import org.junit.Test;
import org.netbeans.jemmy.ComponentChooser;
import org.netbeans.jemmy.operators.*;
import org.netbeans.jemmy.util.NameComponentChooser;

/**
 * @author Christian Schlichtherle
 */
public class AuthenticationPanelIT extends JemmyUtilsWithFile {

    private static final String LABEL_TEXT = "Hello World!";

    private JFrameOperator frame;

    public AuthenticationPanelIT() throws IOException, InterruptedException {
        final JPanel passwdPanel = new JPanel();
        passwdPanel.add(new JLabel(LABEL_TEXT));
        final AuthenticationPanel panel = new AuthenticationPanel();
        panel.setPasswdPanel(passwdPanel);
        frame = showFrameWith(panel);
    }

    @After
    public void disposeFrame() { frame.dispose(); }

    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    @Test
    @Ignore(/* FIXME */ "JFileChooserOperator does not work on Mac OS X.")
    public void testTabbedPane() {
        final ComponentChooser
                keyFileChooser = new NameComponentChooser("keyFileChooser");
        new JTabbedPaneOperator(frame).selectPage(AuthenticationPanel.AUTH_KEY_FILE); // select tab for key files
        new JButtonOperator(frame, keyFileChooser).pushNoBlock(); // open file chooser
        JFileChooserOperator fc = new FileChooserOfWindowOperator(frame);
        fc.cancel();
        fc.getQueueTool().waitEmpty();
        new JTabbedPaneOperator(frame).selectPage(AuthenticationPanel.AUTH_PASSWD); // select tab for passwords
        new JLabelOperator(frame, LABEL_TEXT);

        new JTabbedPaneOperator(frame).selectPage(AuthenticationPanel.AUTH_KEY_FILE); // select tab for key files
        new JButtonOperator(frame, keyFileChooser).pushNoBlock(); // open file chooser
        fc = new FileChooserOfWindowOperator(frame);
        fc.chooseFile(file.getName());
        fc.getQueueTool().waitEmpty();
        JTextFieldOperator tf = new JTextFieldOperator(frame);
        assertEquals(file.getName(), tf.getText());

        new JTabbedPaneOperator(frame).selectPage(AuthenticationPanel.AUTH_PASSWD); // select tab for passwords
        new JLabelOperator(frame, LABEL_TEXT);
    }
}
