/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.key.impl.pbe.swing;

import de.truezip.kernel.key.param.SafePbeParameters;
import de.truezip.kernel.key.impl.pbe.swing.AuthenticationPanel;
import de.truezip.kernel.key.impl.pbe.swing.KeyPanel;
import de.truezip.swing.JemmyUtils;
import java.awt.EventQueue;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.netbeans.jemmy.ComponentChooser;
import org.netbeans.jemmy.operators.*;
import org.netbeans.jemmy.util.NameComponentChooser;

/**
 * @param   <P> The type of the key panel.
 * @author  Christian Schlichtherle
 */
public abstract class KeyPanelTestSuite<P extends KeyPanel> extends JemmyUtils {
    private static final ComponentChooser
            KEY_FILE_CHOOSER = new NameComponentChooser("keyFileChooser");

    private static final String NON_EXISTING_FILE
            = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    protected P panel;
    protected JFrameOperator frame;
    protected JLabelOperator error;

    @Before
    public void setUp() throws InterruptedException {
        panel = newKeyPanel();
        frame = showFrameWith(panel);
        final String text = "error";
        panel.setError(text);
        error = new JLabelOperator(frame, text);
        panel.setError(null);
    }

    protected abstract P newKeyPanel();

    protected abstract SafePbeParameters<?, ?> newPbeParameters();

    @After
    public void tearDown() {
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
    public void testUpdateErrorLabel() {
        panel.setError("This is a test error message!");
        assertFalse(isBlank(error.getText()));
        final JTextFieldOperator tf = new JTextFieldOperator(frame);
        tf.setText("secret");
        //tf.getQueueTool().waitEmpty(WAIT_EMPTY);
        assertTrue(isBlank(error.getText()));

        panel.setError("This is a test error message!");
        assertFalse(isBlank(error.getText()));
        new JTabbedPaneOperator(frame).selectPage(AuthenticationPanel.AUTH_KEY_FILE); // select tab for key files
        new JButtonOperator(frame, KEY_FILE_CHOOSER).push(); // open file chooser
        final JFileChooserOperator fc = new TFileChooserOperator(frame);
        fc.chooseFile(NON_EXISTING_FILE);
        fc.getQueueTool().waitEmpty(WAIT_EMPTY);
        assertTrue(isBlank(error.getText()));
    }

    protected static boolean isBlank(String s) {
        return null == s || s.trim().length() <= 0;
    }

    @Test
    public void testKeyFile() throws InterruptedException {
        final SafePbeParameters<?, ?> param = newPbeParameters();

        new JTabbedPaneOperator(frame).selectPage(AuthenticationPanel.AUTH_KEY_FILE); // select tab for key files
        new JButtonOperator(frame, KEY_FILE_CHOOSER).push(); // open file chooser
        JFileChooserOperator fc = new TFileChooserOperator(frame);
        fc.chooseFile(NON_EXISTING_FILE);
        fc.getQueueTool().waitEmpty(WAIT_EMPTY);
        assertTrue(isBlank(error.getText()));
        assertFalse(updateParam(param));
        assertFalse(isBlank(error.getText()));

        new JButtonOperator(frame, KEY_FILE_CHOOSER).push(); // open file chooser
        fc = new TFileChooserOperator(frame);
        final List<File> files = Arrays.asList(fc.getFiles());
        Collections.shuffle(files);
        for (final File file : files) {
            if (!file.isFile())
                continue;
            fc.setSelectedFile(file);
            fc.approve(); // close file chooser
            if (updateParam(param)) {
                assertNotNull(param.getPassword());
                assertTrue(isBlank(error.getText()));
            } else {
                assertFalse(isBlank(error.getText()));
            }
            return;
        }
        fc.cancel(); // close file chooser
    }

    protected final boolean updateParam(final SafePbeParameters<?, ?> param)
    throws InterruptedException {
        class Update implements Runnable {
            boolean result;

            @Override
            public void run() {
                result = panel.updateParam(param);
            }
        }

        final Update update = new Update();
        try {
            EventQueue.invokeAndWait(update);
        } catch (InvocationTargetException ex) {
            throw new AssertionError(ex);
        }
        return update.result;
    }
}