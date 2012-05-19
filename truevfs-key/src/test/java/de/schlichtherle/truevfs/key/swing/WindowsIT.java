/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truevfs.key.swing;

import de.schlichtherle.truevfs.key.swing.Windows;
import de.schlichtherle.truevfs.key.swing.JemmyUtils;
import java.awt.Window;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.WindowConstants;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * @author  Christian Schlichtherle
 */
public class WindowsIT extends JemmyUtils {

    @Test
    public void testParentWindow() throws InterruptedException {
        runOnEdtNow(new Runnable() {
            @Override
            public void run() {
                Window result = Windows.getParentWindow();
                assertNotNull(result);
                assertFalse(result.isVisible());

                final JFrame frame = new JFrame();
                frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
                final JDialog dialog = new JDialog(frame);
                dialog.pack();
                dialog.setLocationRelativeTo(null);

                assertFalse(dialog.isVisible());
                result = Windows.getParentWindow();
                assertSame(frame, result);

                dialog.setVisible(true);
                result = Windows.getParentWindow();
                assertSame(frame, result);

                dialog.dispose();
                frame.dispose();
            }
        });
    }
}