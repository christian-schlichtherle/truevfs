/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.key.swing;

import net.java.truecommons3.key.swing.io.JemmyUtilsWithFile;
import net.java.truecommons3.key.swing.util.FileChooserOfWindowOperator;
import java.awt.EventQueue;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import net.java.truecommons3.key.spec.prompting.PromptingPbeParameters;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Ignore;
import org.junit.Test;
import org.netbeans.jemmy.ComponentChooser;
import org.netbeans.jemmy.operators.*;
import org.netbeans.jemmy.util.NameComponentChooser;

/**
 * @param  <P> The type of the key panel.
 * @author Christian Schlichtherle
 */
public abstract class KeyPanelTestSuite<P extends KeyPanel>
extends JemmyUtilsWithFile {

    private static final ComponentChooser
            KEY_FILE_CHOOSER = new NameComponentChooser("keyFileChooser");

    protected P panel;
    protected JFrameOperator frame;
    protected JLabelOperator error;

    public KeyPanelTestSuite() throws IOException, InterruptedException {
        panel = newKeyPanel();
        frame = showFrameWith(panel);
        final String text = "error";
        panel.setError(text);
        error = new JLabelOperator(frame, text);
        panel.setError(null);
    }

    protected abstract P newKeyPanel();

    protected abstract PromptingPbeParameters<?, ?> newPbeParameters();

    @After
    public void disposeFrame() { frame.dispose(); }

    @Test
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testResource() {
        final URI id = URI.create("HelloWorld!");
        panel.setResource(id);
        assertEquals(id, panel.getResource());

        new JTextComponentOperator(frame, id.toString());
    }

    @Test
    @Ignore(/* FIXME */ "JFileChooserOperator does not work on Mac OS X.")
    public void testUpdateErrorLabel() {
        panel.setError("This is a test error message!");
        assertFalse(isBlank(error.getText()));
        final JTextFieldOperator tf = new JTextFieldOperator(frame);
        tf.setText("top secret");
        assertTrue(isBlank(error.getText()));

        panel.setError("This is a test error message!");
        assertFalse(isBlank(error.getText()));
        new JTabbedPaneOperator(frame).selectPage(AuthenticationPanel.AUTH_KEY_FILE); // select tab for key files
        new JButtonOperator(frame, KEY_FILE_CHOOSER).pushNoBlock(); // open file chooser
        final JFileChooserOperator fc = new FileChooserOfWindowOperator(frame);
        fc.chooseFile(file.getName());
        fc.getQueueTool().waitEmpty();
        assertTrue(isBlank(error.getText()));
    }

    protected static boolean isBlank(String s) {
        return 0 == Objects.toString(s, "").trim().length();
    }

    @Test
    @Ignore(/* FIXME */ "JFileChooserOperator does not work on Mac OS X.")
    public void testKeyFile() throws InterruptedException {
        final PromptingPbeParameters<?, ?> param = newPbeParameters();

        new JTabbedPaneOperator(frame).selectPage(AuthenticationPanel.AUTH_KEY_FILE); // select tab for key files
        new JButtonOperator(frame, KEY_FILE_CHOOSER).pushNoBlock(); // open file chooser
        JFileChooserOperator fc = new FileChooserOfWindowOperator(frame);
        fc.chooseFile(file.getName());
        fc.getQueueTool().waitEmpty();
        assertTrue(isBlank(error.getText()));
        assertFalse(updateParam(param));
        assertFalse(isBlank(error.getText()));

        new JButtonOperator(frame, KEY_FILE_CHOOSER).pushNoBlock(); // open file chooser
        fc = new FileChooserOfWindowOperator(frame);
        final List<File> files = Arrays.asList(fc.getFiles());
        Collections.shuffle(files);
        for (final File file : files) {
            if (!file.isFile()) continue;
            fc.chooseFile(file.getName());
            fc.getQueueTool().waitEmpty();
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

    protected final boolean updateParam(
            final PromptingPbeParameters<?, ?> param)
    throws InterruptedException {

        class Update implements Runnable {
            boolean result;

            @Override
            public void run() { result = panel.updateParam(param); }
        }

        final Update update = new Update();
        try {
            EventQueue.invokeAndWait(update);
        } catch (final InvocationTargetException ex) {
            throw new AssertionError(ex);
        }
        return update.result;
    }
}
