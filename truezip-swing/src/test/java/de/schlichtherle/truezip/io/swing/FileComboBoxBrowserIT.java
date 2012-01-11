/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.io.swing;

import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.Collator;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import static org.junit.Assert.*;
import org.junit.Test;
import org.netbeans.jemmy.JemmyProperties;
import org.netbeans.jemmy.TestOut;
import org.netbeans.jemmy.operators.JFrameOperator;
import org.netbeans.jemmy.operators.JTextComponentOperator;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public final class FileComboBoxBrowserIT {
    static {
        JemmyProperties.setCurrentOutput(TestOut.getNullOutput()); // shut up!
    }

    private static final Logger logger
            = Logger.getLogger(FileComboBoxBrowserIT.class.getName());

    @Test
    public void testDirectory() {
        final FileComboBoxBrowser browser = new FileComboBoxBrowser();
        final File cur = new File(".");
        File dir;

        dir = browser.getDirectory();
        assertEquals(cur, dir);

        browser.setDirectory(cur);
        dir = browser.getDirectory();
        assertSame(cur, dir);

        browser.setDirectory(null);
        dir = browser.getDirectory();
        assertEquals(cur, dir);
    }

    @Test
    public void testState4Update() {
        final FileComboBoxBrowser browser = new FileComboBoxBrowser();
        try {
            browser.update("");
            fail("Calling the previous method should throw a NullPointerException!");
        } catch (NullPointerException expected) {
        }

        final JComboBox<String> combo = new JComboBox<String>();
        browser.setComboBox(combo);
        browser.update("");
    }

    @Test
    public void testAutoCompletion() throws IOException {
        File dir = new File(".");
        assertAutoCompletion(dir);
        assertAutoCompletion(dir.getCanonicalFile());
    }

    private void assertAutoCompletion(final File dir) {
        final String[] entries = dir.list();
        if (null == entries || entries.length == 0) {
            logger.warning("Current directory does not contain any files - skipping test!");
            return;
        }

        final JComboBox<String> combo = new JComboBox<String>();
        final FileComboBoxBrowser browser = new FileComboBoxBrowser();
        browser.setDirectory(dir);

        for (int i = 0; i < entries.length; i++) {
            final String entry = entries[i];
            for (int j = 0; j < entry.length(); j++)
                assertAutoCompletion(browser, combo, entry.substring(0, j));
        }
    }

    private static void assertAutoCompletion(
            final FileComboBoxBrowser browser,
            final JComboBox<String> combo,
            final String initials) {
        browser.setComboBox(null); // reset
        combo.removeAllItems(); // reset
        combo.setEditable(true);
        combo.setSelectedItem(initials);
        browser.setComboBox(combo); // initialize
        assertEquals(filter(initials), asList(combo));
    }

    private static List<String> filter(final String initials) {
        final String[] entries = new File(".").list(new FilenameFilter() {
            final int l = initials.length();

            @Override
			public boolean accept(File dir, String name) {
                if (name.length() >= l)
                    return initials.equalsIgnoreCase(name.substring(0, l));
                else
                    return false;
            }
        });
        Arrays.sort(entries, Collator.getInstance());
        return Arrays.asList(entries);
    }

    private static List<Object> asList(final JComboBox<String> combo) {
        final List<Object> list = new LinkedList<Object>();
        final ComboBoxModel<String> model = combo.getModel();
        for (int i = 0, l = model.getSize(); i < l; i++)
            list.add(model.getElementAt(i));
        return list;
    }

    @Test
    // TODO: Complete this test!
    public void testGUI() throws InterruptedException, InvocationTargetException {
        new FileComboBoxPanel(null).run();

        final JFrameOperator frame = new JFrameOperator();
        final JTextComponentOperator tc0 = new JTextComponentOperator(frame, 0);
        final JTextComponentOperator tc1 = new JTextComponentOperator(frame, 1);

        // Type a character in tc0, then check that it's appearing in tc1.
        tc0.typeText("?");
        assertEquals("?", tc1.getText());

        // Clear character in tc1, then ensure that its cleared in tc0, too.
        tc1.clearText();
        assertEquals("", tc0.getText());

        // Select first element in list of tc0 (entry in current directory),
        // if any, and check its appearance in tc1, too.
        tc0.pressKey(KeyEvent.VK_DOWN);
        final String child = tc0.getText();
        assertEquals(child, tc1.getText());

        frame.dispose();
    }
}
